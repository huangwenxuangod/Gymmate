from __future__ import annotations

import json
import os
from datetime import datetime

import httpx
from fastapi import FastAPI, Header, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware

from .database import (
    generate_dev_code,
    generate_session_token,
    get_connection,
    init_db,
    parse_json_field,
    rows_to_dicts,
)
from .schemas import (
    Exercise,
    ExerciseListResponse,
    ExerciseRuleConfig,
    ExerciseRuleListResponse,
    FeedbackRequest,
    FeedbackResponse,
    LoginRequest,
    LoginResponse,
    SelectPlanRequest,
    SendOtpRequest,
    SendOtpResponse,
    SessionCreatedResponse,
    TrainingPlan,
    TrainingPlanListResponse,
    UpdateProfileRequest,
    UserProfile,
    WorkoutHistoryItem,
    WorkoutHistoryResponse,
    WorkoutTrendResponse,
    WorkoutSessionCreate,
    WorkoutSessionFinish,
)


app = FastAPI(title="Gymmate API", version="0.1.0")

def parse_allowed_origins() -> list[str]:
    raw = os.getenv("GYMMATE_ALLOWED_ORIGINS", "*").strip()
    if not raw:
        return ["*"]
    if raw == "*":
        return ["*"]
    return [item.strip() for item in raw.split(",") if item.strip()]


app.add_middleware(
    CORSMiddleware,
    allow_origins=parse_allowed_origins(),
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.on_event("startup")
def on_startup() -> None:
    init_db()


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


def user_from_row(row: dict) -> UserProfile:
    return UserProfile(
        id=row["id"],
        phone=row["phone"],
        nickname=row["nickname"],
        gender=row["gender"],
        height_cm=row["height_cm"],
        weight_kg=row["weight_kg"],
        fitness_goal=row["fitness_goal"],
        training_level=row["training_level"],
        current_plan_id=row["current_plan_id"],
    )


def resolve_user(authorization: str | None) -> UserProfile | None:
    if not authorization or not authorization.startswith("Bearer "):
        return None
    token = authorization.removeprefix("Bearer ").strip()
    if not token:
        return None

    connection = get_connection()
    row = connection.execute(
        """
        SELECT u.*
        FROM user_sessions s
        JOIN users u ON u.id = s.user_id
        WHERE s.token = ?
        """,
        (token,),
    ).fetchone()
    connection.close()
    return user_from_row(dict(row)) if row else None


def exercise_from_row(row: dict) -> Exercise:
    return Exercise(
        id=row["id"],
        name=row["name"],
        category=row["category"],
        target_muscles=parse_json_field(row["target_muscles"]),
        description=row["description"],
        standard_steps=parse_json_field(row["standard_steps"]),
        common_errors=parse_json_field(row["common_errors"]),
        correction_tips=parse_json_field(row["correction_tips"]),
        media_url=row["media_url"],
        difficulty_level=row["difficulty_level"],
    )


def exercise_rule_from_row(row: dict) -> ExerciseRuleConfig:
    return ExerciseRuleConfig(
        exercise_name=row["exercise_name"],
        camera_view=row["camera_view"],
        required_landmarks=parse_json_field(row["required_landmarks"]),
        start_cue=row["start_cue"],
        end_cue=row["end_cue"],
        count_cue=row["count_cue"],
        hard_fails=parse_json_field(row["hard_fails"]),
        soft_warnings=parse_json_field(row["soft_warnings"]),
    )


@app.post("/auth/send-code", response_model=SendOtpResponse)
def send_code(payload: SendOtpRequest) -> SendOtpResponse:
    code = generate_dev_code()
    connection = get_connection()
    connection.execute("DELETE FROM login_otps WHERE phone = ?", (payload.phone,))
    connection.execute(
        "INSERT INTO login_otps (phone, code) VALUES (?, ?)",
        (payload.phone, code),
    )
    connection.commit()
    connection.close()
    return SendOtpResponse(success=True, dev_code=code)


@app.post("/auth/login", response_model=LoginResponse)
def login(payload: LoginRequest) -> LoginResponse:
    connection = get_connection()
    otp = connection.execute(
        """
        SELECT code FROM login_otps
        WHERE phone = ?
        ORDER BY id DESC
        LIMIT 1
        """,
        (payload.phone,),
    ).fetchone()
    if otp is None or otp["code"] != payload.code:
        connection.close()
        raise HTTPException(status_code=400, detail="验证码错误")

    user = connection.execute(
        "SELECT * FROM users WHERE phone = ?",
        (payload.phone,),
    ).fetchone()
    if user is None:
        connection.execute(
            """
            INSERT INTO users (phone, nickname, fitness_goal, training_level)
            VALUES (?, ?, ?, ?)
            """,
            (payload.phone, f"用户{payload.phone[-4:]}", "增肌入门", "beginner"),
        )
        user = connection.execute(
            "SELECT * FROM users WHERE phone = ?",
            (payload.phone,),
        ).fetchone()

    token = generate_session_token()
    connection.execute(
        "INSERT INTO user_sessions (user_id, token) VALUES (?, ?)",
        (user["id"], token),
    )
    connection.commit()
    connection.close()
    return LoginResponse(token=token, user=user_from_row(dict(user)))


@app.get("/me", response_model=UserProfile)
def get_me(authorization: str | None = Header(default=None)) -> UserProfile:
    user = resolve_user(authorization)
    if user is None:
        raise HTTPException(status_code=401, detail="未登录")
    return user


@app.put("/me", response_model=UserProfile)
def update_me(
    payload: UpdateProfileRequest,
    authorization: str | None = Header(default=None),
) -> UserProfile:
    user = resolve_user(authorization)
    if user is None:
        raise HTTPException(status_code=401, detail="未登录")

    connection = get_connection()
    connection.execute(
        """
        UPDATE users
        SET nickname = ?, gender = ?, height_cm = ?, weight_kg = ?, fitness_goal = ?, training_level = ?
        WHERE id = ?
        """,
        (
            payload.nickname,
            payload.gender,
            payload.height_cm,
            payload.weight_kg,
            payload.fitness_goal,
            payload.training_level,
            user.id,
        ),
    )
    updated = connection.execute("SELECT * FROM users WHERE id = ?", (user.id,)).fetchone()
    connection.commit()
    connection.close()
    return user_from_row(dict(updated))


@app.get("/exercises", response_model=ExerciseListResponse)
def list_exercises(
    q: str | None = Query(default=None),
    category: str | None = Query(default=None),
) -> ExerciseListResponse:
    connection = get_connection()
    cursor = connection.cursor()

    if q and category:
        like_query = f"%{q}%"
        rows = cursor.execute(
            """
            SELECT DISTINCT e.*
            FROM exercises e
            LEFT JOIN exercise_search s ON e.id = s.rowid
            WHERE e.category = ?
              AND (
                    e.name LIKE ?
                 OR e.category LIKE ?
                 OR e.description LIKE ?
                 OR e.target_muscles LIKE ?
                 OR s.common_errors LIKE ?
              )
            ORDER BY e.id
            """,
            (category, like_query, like_query, like_query, like_query, like_query),
        ).fetchall()
    elif q:
        like_query = f"%{q}%"
        rows = cursor.execute(
            """
            SELECT DISTINCT e.*
            FROM exercises e
            LEFT JOIN exercise_search s ON e.id = s.rowid
            WHERE e.name LIKE ?
               OR e.category LIKE ?
               OR e.description LIKE ?
               OR e.target_muscles LIKE ?
               OR s.common_errors LIKE ?
            ORDER BY e.id
            """,
            (like_query, like_query, like_query, like_query, like_query),
        ).fetchall()
    elif category:
        rows = cursor.execute(
            "SELECT * FROM exercises WHERE category = ? ORDER BY id",
            (category,),
        ).fetchall()
    else:
        rows = cursor.execute("SELECT * FROM exercises ORDER BY id").fetchall()

    connection.close()
    return ExerciseListResponse(items=[exercise_from_row(dict(row)) for row in rows])


@app.get("/exercise-categories")
def list_exercise_categories() -> dict[str, list[str]]:
    connection = get_connection()
    rows = connection.execute(
        "SELECT DISTINCT category FROM exercises ORDER BY category",
    ).fetchall()
    connection.close()
    return {"items": [row["category"] for row in rows]}


@app.get("/exercises/{exercise_id}", response_model=Exercise)
def get_exercise(exercise_id: int) -> Exercise:
    connection = get_connection()
    row = connection.execute(
        "SELECT * FROM exercises WHERE id = ?",
        (exercise_id,),
    ).fetchone()
    connection.close()
    if row is None:
        raise HTTPException(status_code=404, detail="Exercise not found")
    return exercise_from_row(dict(row))


@app.get("/exercise-rules", response_model=ExerciseRuleListResponse)
def list_exercise_rules() -> ExerciseRuleListResponse:
    connection = get_connection()
    rows = rows_to_dicts(
        connection.execute(
            "SELECT * FROM exercise_rule_configs ORDER BY id",
        ).fetchall(),
    )
    connection.close()
    return ExerciseRuleListResponse(
        items=[exercise_rule_from_row(row) for row in rows],
    )


@app.get("/exercise-rules/{exercise_name}", response_model=ExerciseRuleConfig)
def get_exercise_rule(exercise_name: str) -> ExerciseRuleConfig:
    connection = get_connection()
    row = connection.execute(
        "SELECT * FROM exercise_rule_configs WHERE exercise_name = ?",
        (exercise_name,),
    ).fetchone()
    connection.close()
    if row is None:
        raise HTTPException(status_code=404, detail="Exercise rule not found")
    return exercise_rule_from_row(dict(row))


@app.get("/exercise-rule-by-id/{exercise_id}", response_model=ExerciseRuleConfig)
def get_exercise_rule_by_id(exercise_id: int) -> ExerciseRuleConfig:
    connection = get_connection()
    row = connection.execute(
        """
        SELECT r.*
        FROM exercise_rule_configs r
        JOIN exercises e ON e.name = r.exercise_name
        WHERE e.id = ?
        """,
        (exercise_id,),
    ).fetchone()
    connection.close()
    if row is None:
        raise HTTPException(status_code=404, detail="Exercise rule not found")
    return exercise_rule_from_row(dict(row))


@app.get("/plans", response_model=TrainingPlanListResponse)
def list_plans() -> TrainingPlanListResponse:
    connection = get_connection()
    rows = rows_to_dicts(connection.execute("SELECT * FROM training_plans ORDER BY id").fetchall())
    connection.close()
    return TrainingPlanListResponse(
        items=[
            TrainingPlan(
                id=row["id"],
                name=row["name"],
                goal_type=row["goal_type"],
                level=row["level"],
                description=row["description"],
                schedule=parse_json_field(row["schedule"]),
            )
            for row in rows
        ]
    )


@app.post("/user/plans/select", response_model=UserProfile)
def select_current_plan(
    payload: SelectPlanRequest,
    authorization: str | None = Header(default=None),
) -> UserProfile:
    user = resolve_user(authorization)
    if user is None:
        raise HTTPException(status_code=401, detail="未登录")

    connection = get_connection()
    plan = connection.execute(
        "SELECT id FROM training_plans WHERE id = ?",
        (payload.training_plan_id,),
    ).fetchone()
    if plan is None:
        connection.close()
        raise HTTPException(status_code=404, detail="训练计划不存在")

    connection.execute(
        "UPDATE users SET current_plan_id = ? WHERE id = ?",
        (payload.training_plan_id, user.id),
    )
    updated = connection.execute("SELECT * FROM users WHERE id = ?", (user.id,)).fetchone()
    connection.commit()
    connection.close()
    return user_from_row(dict(updated))


@app.get("/user/plans/current", response_model=TrainingPlan)
def get_current_plan(authorization: str | None = Header(default=None)) -> TrainingPlan:
    user = resolve_user(authorization)
    if user is None:
        raise HTTPException(status_code=401, detail="未登录")
    if user.current_plan_id is None:
        raise HTTPException(status_code=404, detail="当前未选择训练计划")

    connection = get_connection()
    row = connection.execute(
        "SELECT * FROM training_plans WHERE id = ?",
        (user.current_plan_id,),
    ).fetchone()
    connection.close()
    if row is None:
        raise HTTPException(status_code=404, detail="训练计划不存在")
    return TrainingPlan(
        id=row["id"],
        name=row["name"],
        goal_type=row["goal_type"],
        level=row["level"],
        description=row["description"],
        schedule=parse_json_field(row["schedule"]),
    )


@app.post("/workout/sessions", response_model=SessionCreatedResponse)
def create_session(
    payload: WorkoutSessionCreate,
    authorization: str | None = Header(default=None),
) -> SessionCreatedResponse:
    user = resolve_user(authorization)
    if user is None:
        raise HTTPException(status_code=401, detail="未登录")
    connection = get_connection()
    cursor = connection.cursor()
    cursor.execute(
        """
        INSERT INTO workout_sessions (user_id, exercise_id, started_at)
        VALUES (?, ?, ?)
        """,
        (user.id, payload.exercise_id, payload.started_at.isoformat()),
    )
    session_id = cursor.lastrowid
    connection.commit()
    connection.close()
    return SessionCreatedResponse(session_id=session_id)


@app.post("/workout/sessions/{session_id}/finish")
def finish_session(
    session_id: int,
    payload: WorkoutSessionFinish,
    authorization: str | None = Header(default=None),
) -> dict[str, str]:
    user = resolve_user(authorization)
    if user is None:
        raise HTTPException(status_code=401, detail="未登录")

    connection = get_connection()
    cursor = connection.cursor()
    existing = cursor.execute(
        "SELECT id FROM workout_sessions WHERE id = ? AND user_id = ?",
        (session_id, user.id),
    ).fetchone()
    if existing is None:
        connection.close()
        raise HTTPException(status_code=404, detail="Session not found")

    cursor.execute(
        """
        UPDATE workout_sessions
        SET ended_at = ?, total_reps = ?, avg_score = ?, error_tags = ?, summary_text = ?
        WHERE id = ?
        """,
        (
            payload.ended_at.isoformat(),
            payload.total_reps,
            payload.avg_score,
            json.dumps(payload.error_tags, ensure_ascii=False),
            payload.summary_text,
            session_id,
        ),
    )
    connection.commit()
    connection.close()
    return {"status": "saved"}


@app.get("/workout/history", response_model=WorkoutHistoryResponse)
def workout_history(authorization: str | None = Header(default=None)) -> WorkoutHistoryResponse:
    user = resolve_user(authorization)
    if user is None:
        raise HTTPException(status_code=401, detail="未登录")

    connection = get_connection()
    rows = rows_to_dicts(
        connection.execute(
            """
            SELECT
                s.id,
                e.name AS exercise_name,
                s.started_at,
                s.ended_at,
                s.total_reps,
                s.avg_score,
                s.error_tags,
                s.summary_text
            FROM workout_sessions s
            JOIN exercises e ON e.id = s.exercise_id
            WHERE s.ended_at IS NOT NULL
              AND s.user_id = ?
            ORDER BY s.started_at DESC
            """,
            (user.id,),
        ).fetchall()
    )
    connection.close()
    return WorkoutHistoryResponse(
        items=[
            WorkoutHistoryItem(
                id=row["id"],
                exercise_name=row["exercise_name"],
                started_at=datetime.fromisoformat(row["started_at"]),
                ended_at=datetime.fromisoformat(row["ended_at"]),
                total_reps=row["total_reps"],
                avg_score=row["avg_score"],
                error_tags=parse_json_field(row["error_tags"]),
                summary_text=row["summary_text"],
            )
            for row in rows
        ]
    )


@app.get("/workout/trends", response_model=WorkoutTrendResponse)
def workout_trends(
    days: int = Query(default=7, ge=7, le=30),
    authorization: str | None = Header(default=None),
) -> WorkoutTrendResponse:
    user = resolve_user(authorization)
    if user is None:
        raise HTTPException(status_code=401, detail="未登录")

    connection = get_connection()
    rows = rows_to_dicts(
        connection.execute(
            """
            SELECT
                substr(started_at, 1, 10) AS date,
                COUNT(*) AS sessions,
                CAST(AVG(avg_score) AS INTEGER) AS avg_score
            FROM workout_sessions
            WHERE ended_at IS NOT NULL
              AND user_id = ?
            GROUP BY substr(started_at, 1, 10)
            ORDER BY date DESC
            LIMIT ?
            """,
            (user.id, days),
        ).fetchall()
    )
    connection.close()
    return WorkoutTrendResponse(items=list(reversed(rows)))


@app.post("/ai/feedback", response_model=FeedbackResponse)
async def ai_feedback(payload: FeedbackRequest) -> FeedbackResponse:
    api_key = os.getenv("DEEPSEEK_API_KEY")
    if not api_key:
        return FeedbackResponse(
            summary=f"{payload.exercise_name} 本组完成度不错，重点先把动作稳定性做扎实。",
            top_errors=payload.error_tags[:3],
            next_time_focus=payload.focus or "下一组优先减少借力，控制离心节奏。",
            coach_tip="把节奏放慢一点，先做标准，再追求重量和次数。",
        )

    base_url = os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com").rstrip("/")
    model = os.getenv("DEEPSEEK_MODEL", "deepseek-v4-flash")
    timeout_seconds = float(os.getenv("DEEPSEEK_TIMEOUT_SECONDS", "20"))
    prompt = (
        "你是一名严格但友好的健身教练。"
        "请根据动作名、评分和错误标签，输出一个 JSON 对象。"
        "JSON 只允许包含 summary、top_errors、next_time_focus、coach_tip 这四个字段。"
        "top_errors 必须是字符串数组，其余字段必须是字符串。"
        "不要输出 markdown，不要输出额外解释，不要输出医学建议，不要编造未提供的信息。"
    )
    user_content = {
        "exercise_name": payload.exercise_name,
        "avg_score": payload.avg_score,
        "error_tags": payload.error_tags,
        "focus": payload.focus,
    }

    async with httpx.AsyncClient(timeout=timeout_seconds) as client:
        response = await client.post(
            f"{base_url}/chat/completions",
            headers={
                "Authorization": f"Bearer {api_key}",
                "Content-Type": "application/json",
            },
            json={
                "model": model,
                "messages": [
                    {"role": "system", "content": prompt},
                    {"role": "user", "content": json.dumps(user_content, ensure_ascii=False)},
                ],
                "response_format": {"type": "json_object"},
                "thinking": {"type": "disabled"},
                "max_tokens": 512,
                "temperature": 0.2,
            },
        )
        response.raise_for_status()
        message = response.json()["choices"][0]["message"]
        content = message.get("content", "")

    try:
        parsed = json.loads(content)
        return FeedbackResponse(**parsed)
    except (json.JSONDecodeError, TypeError, ValueError) as exc:
        raise HTTPException(status_code=502, detail=f"LLM JSON parse failed: {exc}") from exc
