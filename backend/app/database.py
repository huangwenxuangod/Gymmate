from __future__ import annotations

import json
import secrets
import sqlite3
import os
from pathlib import Path
from typing import Any, Iterable


DEFAULT_DB_PATH = Path(__file__).resolve().parent.parent / "gymmate.db"
DB_PATH = Path(os.getenv("GYMMATE_DB_PATH", str(DEFAULT_DB_PATH))).expanduser().resolve()


EXERCISE_SEEDS = [
    {
        "name": "杠铃深蹲",
        "category": "下肢",
        "target_muscles": ["股四头肌", "臀大肌", "核心"],
        "description": "用深蹲建立下肢力量和稳定性。",
        "standard_steps": ["双脚与肩同宽站立", "保持核心收紧下蹲", "髋部低于膝部后起身"],
        "common_errors": ["膝内扣", "深度不足", "腰背松散"],
        "correction_tips": ["脚掌均匀发力", "注意膝盖朝脚尖方向", "起身时保持胸口打开"],
        "media_url": "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?auto=format&fit=crop&w=1200&q=80",
        "difficulty_level": "beginner",
    },
    {
        "name": "哑铃推肩",
        "category": "肩部",
        "target_muscles": ["三角肌", "肱三头肌"],
        "description": "提升肩部力量的基础推举动作。",
        "standard_steps": ["哑铃举至耳侧", "向上推至手臂接近伸直", "缓慢还原"],
        "common_errors": ["耸肩", "下放过快", "腰部过度后仰"],
        "correction_tips": ["保持肩膀下沉", "离心控制 2 秒", "收紧腹部和臀部"],
        "media_url": "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&w=1200&q=80",
        "difficulty_level": "beginner",
    },
    {
        "name": "哑铃侧平举",
        "category": "肩部",
        "target_muscles": ["三角肌中束"],
        "description": "塑造肩部宽度的经典孤立动作。",
        "standard_steps": ["微屈肘", "向两侧抬至肩高", "缓慢下放"],
        "common_errors": ["借力摆动", "抬手过高", "耸肩"],
        "correction_tips": ["缩小重量", "专注肩部发力", "抬到肩高即可"],
        "media_url": "https://images.unsplash.com/photo-1583454110551-21f2fa2afe61?auto=format&fit=crop&w=1200&q=80",
        "difficulty_level": "beginner",
    },
    {
        "name": "高位下拉",
        "category": "背部",
        "target_muscles": ["背阔肌", "肱二头肌"],
        "description": "帮助新手建立背部发力感的入门动作。",
        "standard_steps": ["握距略宽于肩", "下拉至锁骨附近", "缓慢还原"],
        "common_errors": ["身体后仰过多", "耸肩", "还原不完整"],
        "correction_tips": ["保持胸口上提", "肩胛先下沉", "完整伸展背阔肌"],
        "media_url": "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&w=1200&q=80",
        "difficulty_level": "beginner",
    },
    {
        "name": "坐姿绳索划船",
        "category": "背部",
        "target_muscles": ["背阔肌", "菱形肌", "肱二头肌"],
        "description": "改善背部收缩控制的稳定动作。",
        "standard_steps": ["保持胸口挺起", "拉柄至肚脐附近", "肩胛向后收紧"],
        "common_errors": ["圆肩", "借力后仰", "前伸不足"],
        "correction_tips": ["先收肩胛再拉肘", "控制躯干稳定", "回程做满"],
        "media_url": "https://images.unsplash.com/photo-1599058917212-d750089bc07e?auto=format&fit=crop&w=1200&q=80",
        "difficulty_level": "beginner",
    },
    {
        "name": "杠铃卧推",
        "category": "胸部",
        "target_muscles": ["胸大肌", "三角肌前束", "肱三头肌"],
        "description": "基础上肢推力动作。",
        "standard_steps": ["肩胛后收下沉", "杠铃下放至胸线", "稳定推起"],
        "common_errors": ["肘外展过多", "杠路径不稳", "耸肩"],
        "correction_tips": ["保持肩胛稳定", "手腕叠在肘上方", "发力时脚跟踩实"],
        "media_url": "https://images.unsplash.com/photo-1518611012118-696072aa579a?auto=format&fit=crop&w=1200&q=80",
        "difficulty_level": "intermediate",
    },
    {
        "name": "器械推胸",
        "category": "胸部",
        "target_muscles": ["胸大肌", "肱三头肌"],
        "description": "轨迹更稳定，适合新手建立胸推发力。",
        "standard_steps": ["座椅调至手柄与胸线平齐", "推至接近伸直", "缓慢回程"],
        "common_errors": ["耸肩", "回程太短", "手腕弯折"],
        "correction_tips": ["肩膀远离耳朵", "完整做程", "保持手腕中立"],
        "media_url": "https://images.unsplash.com/photo-1517344884509-a0c97ec11bcc?auto=format&fit=crop&w=1200&q=80",
        "difficulty_level": "beginner",
    },
    {
        "name": "器械腿屈伸",
        "category": "下肢",
        "target_muscles": ["股四头肌"],
        "description": "新手友好的股四头肌孤立动作。",
        "standard_steps": ["坐稳贴背", "小腿抬起至接近伸直", "慢速下放"],
        "common_errors": ["甩腿", "顶峰停留不足", "回程过快"],
        "correction_tips": ["不要借力", "顶峰停 1 秒", "离心控制节奏"],
        "media_url": "https://images.unsplash.com/photo-1571902943202-507ec2618e8f?auto=format&fit=crop&w=1200&q=80",
        "difficulty_level": "beginner",
    },
]


PLAN_SEEDS = [
    {
        "name": "增肌入门 A",
        "goal_type": "增肌入门",
        "level": "beginner",
        "description": "每周 3 练，优先建立动作模式和基础容量。",
        "schedule": ["周一：胸肩三头", "周三：背二头", "周五：腿和核心"],
    },
    {
        "name": "减脂塑形 A",
        "goal_type": "减脂塑形",
        "level": "beginner",
        "description": "基础力量训练搭配低强度有氧。",
        "schedule": ["周一：全身力量", "周三：下肢 + 有氧", "周六：上肢 + 有氧"],
    },
    {
        "name": "基础力量提升 A",
        "goal_type": "基础力量提升",
        "level": "intermediate",
        "description": "以深蹲、推举、拉力动作为核心的轻进阶模板。",
        "schedule": ["周一：深蹲主练", "周三：推举主练", "周五：拉力主练"],
    },
]

RULE_CONFIG_SEEDS = [
    {
        "exercise_name": "杠铃深蹲",
        "camera_view": "侧前 45°",
        "required_landmarks": ["left_hip", "right_hip", "left_knee", "right_knee", "left_ankle", "right_ankle"],
        "start_cue": "站直并保持膝、髋、踝完整入镜",
        "end_cue": "下蹲到底，膝角进入 95° 到 110°",
        "count_cue": "站立 -> 底部 -> 重新站直算 1 次",
        "hard_fails": ["深度不足"],
        "soft_warnings": ["膝内扣", "躯干前倾过多"],
    },
    {
        "exercise_name": "哑铃推肩",
        "camera_view": "正前",
        "required_landmarks": ["left_shoulder", "right_shoulder", "left_elbow", "right_elbow", "left_wrist", "right_wrist", "left_ear", "right_ear"],
        "start_cue": "手腕回到肩旁准备位",
        "end_cue": "手腕过耳线，肘接近伸直",
        "count_cue": "肩旁 -> 头顶 -> 回肩旁算 1 次",
        "hard_fails": ["推举高度不足"],
        "soft_warnings": ["左右不稳定", "手臂路径前飘"],
    },
    {
        "exercise_name": "哑铃侧平举",
        "camera_view": "正前",
        "required_landmarks": ["left_shoulder", "right_shoulder", "left_elbow", "right_elbow", "left_wrist", "right_wrist", "left_hip", "right_hip"],
        "start_cue": "手落髋侧",
        "end_cue": "抬到肩高附近",
        "count_cue": "髋侧 -> 肩高 -> 回髋侧算 1 次",
        "hard_fails": ["抬手高度不足"],
        "soft_warnings": ["左右不对称", "借力摆动"],
    },
    {
        "exercise_name": "坐姿绳索划船",
        "camera_view": "前方 45°",
        "required_landmarks": ["left_shoulder", "right_shoulder", "left_elbow", "right_elbow", "left_wrist", "right_wrist"],
        "start_cue": "手臂前伸",
        "end_cue": "肘回拉到躯干后方",
        "count_cue": "前伸 -> 回拉 -> 前伸算 1 次",
        "hard_fails": ["拉肘不充分"],
        "soft_warnings": ["左右不对称", "回拉不紧"],
    },
    {
        "exercise_name": "高位下拉",
        "camera_view": "前方 45°",
        "required_landmarks": ["left_shoulder", "right_shoulder", "left_elbow", "right_elbow", "left_wrist", "right_wrist"],
        "start_cue": "手臂完全上举",
        "end_cue": "拉到上胸附近并收肘",
        "count_cue": "完全伸展 -> 下拉 -> 回完全伸展算 1 次",
        "hard_fails": ["下拉不充分"],
        "soft_warnings": ["肘部收得不够", "左右发力不一致"],
    },
    {
        "exercise_name": "杠铃卧推",
        "camera_view": "前方 45°",
        "required_landmarks": ["left_shoulder", "right_shoulder", "left_elbow", "right_elbow", "left_wrist", "right_wrist"],
        "start_cue": "顶部或底部终点稳定",
        "end_cue": "到达另一终点",
        "count_cue": "顶部 <-> 底部闭环算 1 次",
        "hard_fails": ["ROM 不完整"],
        "soft_warnings": ["肘外展过多", "左右路径不稳"],
    },
    {
        "exercise_name": "器械推胸",
        "camera_view": "前方 45°",
        "required_landmarks": ["left_shoulder", "right_shoulder", "left_elbow", "right_elbow", "left_wrist", "right_wrist"],
        "start_cue": "手柄回到底部",
        "end_cue": "推到顶端",
        "count_cue": "底部 -> 顶部 -> 底部算 1 次",
        "hard_fails": ["推起不充分"],
        "soft_warnings": ["左右不稳定", "肘部展开过多"],
    },
    {
        "exercise_name": "器械腿屈伸",
        "camera_view": "侧前 45°",
        "required_landmarks": ["left_hip", "right_hip", "left_knee", "right_knee", "left_ankle", "right_ankle"],
        "start_cue": "屈膝位稳定",
        "end_cue": "膝接近伸直",
        "count_cue": "屈膝 -> 伸直到顶 -> 回屈膝算 1 次",
        "hard_fails": ["伸膝不充分"],
        "soft_warnings": ["双腿节奏不一致"],
    },
]


def get_connection() -> sqlite3.Connection:
    connection = sqlite3.connect(DB_PATH)
    connection.row_factory = sqlite3.Row
    connection.execute("PRAGMA journal_mode=WAL;")
    connection.execute("PRAGMA foreign_keys=ON;")
    return connection


def init_db() -> None:
    connection = get_connection()
    cursor = connection.cursor()

    cursor.executescript(
        """
        CREATE TABLE IF NOT EXISTS exercises (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            category TEXT NOT NULL,
            target_muscles TEXT NOT NULL,
            description TEXT NOT NULL,
            standard_steps TEXT NOT NULL,
            common_errors TEXT NOT NULL,
            correction_tips TEXT NOT NULL,
            media_url TEXT NOT NULL,
            difficulty_level TEXT NOT NULL
        );

        CREATE VIRTUAL TABLE IF NOT EXISTS exercise_search USING fts5(
            name,
            category,
            target_muscles,
            description,
            common_errors,
            content='exercises',
            content_rowid='id'
        );

        CREATE TABLE IF NOT EXISTS training_plans (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            goal_type TEXT NOT NULL,
            level TEXT NOT NULL,
            description TEXT NOT NULL,
            schedule TEXT NOT NULL
        );

        CREATE TABLE IF NOT EXISTS exercise_rule_configs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            exercise_name TEXT NOT NULL UNIQUE,
            camera_view TEXT NOT NULL,
            required_landmarks TEXT NOT NULL,
            start_cue TEXT NOT NULL,
            end_cue TEXT NOT NULL,
            count_cue TEXT NOT NULL,
            hard_fails TEXT NOT NULL,
            soft_warnings TEXT NOT NULL
        );

        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            phone TEXT NOT NULL UNIQUE,
            nickname TEXT NOT NULL,
            gender TEXT,
            height_cm INTEGER,
            weight_kg INTEGER,
            fitness_goal TEXT,
            training_level TEXT,
            current_plan_id INTEGER,
            created_at TEXT DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (current_plan_id) REFERENCES training_plans(id)
        );

        CREATE TABLE IF NOT EXISTS login_otps (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            phone TEXT NOT NULL,
            code TEXT NOT NULL,
            created_at TEXT DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS user_sessions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL,
            token TEXT NOT NULL UNIQUE,
            created_at TEXT DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id)
        );

        CREATE TABLE IF NOT EXISTS workout_sessions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER,
            exercise_id INTEGER NOT NULL,
            started_at TEXT NOT NULL,
            ended_at TEXT,
            total_reps INTEGER DEFAULT 0,
            avg_score INTEGER DEFAULT 0,
            error_tags TEXT DEFAULT '[]',
            summary_text TEXT DEFAULT '',
            FOREIGN KEY (exercise_id) REFERENCES exercises(id),
            FOREIGN KEY (user_id) REFERENCES users(id)
        );
        """
    )

    ensure_column(cursor, "workout_sessions", "user_id", "INTEGER")

    seed_if_empty(cursor)
    connection.commit()
    connection.close()


def seed_if_empty(cursor: sqlite3.Cursor) -> None:
    exercise_count = cursor.execute("SELECT COUNT(*) FROM exercises").fetchone()[0]
    if exercise_count == 0:
        for seed in EXERCISE_SEEDS:
            cursor.execute(
                """
                INSERT INTO exercises (
                    name, category, target_muscles, description, standard_steps,
                    common_errors, correction_tips, media_url, difficulty_level
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    seed["name"],
                    seed["category"],
                    json.dumps(seed["target_muscles"], ensure_ascii=False),
                    seed["description"],
                    json.dumps(seed["standard_steps"], ensure_ascii=False),
                    json.dumps(seed["common_errors"], ensure_ascii=False),
                    json.dumps(seed["correction_tips"], ensure_ascii=False),
                    seed["media_url"],
                    seed["difficulty_level"],
                ),
            )
            row_id = cursor.lastrowid
            cursor.execute(
                """
                INSERT INTO exercise_search (
                    rowid, name, category, target_muscles, description, common_errors
                ) VALUES (?, ?, ?, ?, ?, ?)
                """,
                (
                    row_id,
                    seed["name"],
                    seed["category"],
                    " ".join(seed["target_muscles"]),
                    seed["description"],
                    " ".join(seed["common_errors"]),
                ),
            )

    plan_count = cursor.execute("SELECT COUNT(*) FROM training_plans").fetchone()[0]
    if plan_count == 0:
        for seed in PLAN_SEEDS:
            cursor.execute(
                """
                INSERT INTO training_plans (name, goal_type, level, description, schedule)
                VALUES (?, ?, ?, ?, ?)
                """,
                (
                    seed["name"],
                    seed["goal_type"],
                    seed["level"],
                    seed["description"],
                    json.dumps(seed["schedule"], ensure_ascii=False),
                ),
            )

    rule_count = cursor.execute("SELECT COUNT(*) FROM exercise_rule_configs").fetchone()[0]
    if rule_count == 0:
        for seed in RULE_CONFIG_SEEDS:
            cursor.execute(
                """
                INSERT INTO exercise_rule_configs (
                    exercise_name, camera_view, required_landmarks,
                    start_cue, end_cue, count_cue, hard_fails, soft_warnings
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    seed["exercise_name"],
                    seed["camera_view"],
                    json.dumps(seed["required_landmarks"], ensure_ascii=False),
                    seed["start_cue"],
                    seed["end_cue"],
                    seed["count_cue"],
                    json.dumps(seed["hard_fails"], ensure_ascii=False),
                    json.dumps(seed["soft_warnings"], ensure_ascii=False),
                ),
            )


def parse_json_field(value: str) -> Any:
    return json.loads(value)


def rows_to_dicts(rows: Iterable[sqlite3.Row]) -> list[dict[str, Any]]:
    return [dict(row) for row in rows]


def ensure_column(cursor: sqlite3.Cursor, table_name: str, column_name: str, column_type: str) -> None:
    columns = cursor.execute(f"PRAGMA table_info({table_name})").fetchall()
    existing = {column[1] for column in columns}
    if column_name not in existing:
        cursor.execute(f"ALTER TABLE {table_name} ADD COLUMN {column_name} {column_type}")


def generate_dev_code() -> str:
    return f"{secrets.randbelow(900000) + 100000}"


def generate_session_token() -> str:
    return secrets.token_urlsafe(32)
