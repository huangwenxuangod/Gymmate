from __future__ import annotations

from datetime import datetime
from typing import List, Optional

from pydantic import BaseModel, Field


class Exercise(BaseModel):
    id: int
    name: str
    category: str
    target_muscles: List[str]
    description: str
    standard_steps: List[str]
    common_errors: List[str]
    correction_tips: List[str]
    media_url: str
    difficulty_level: str


class ExerciseRuleConfig(BaseModel):
    exercise_name: str
    camera_view: str
    required_landmarks: List[str]
    start_cue: str
    end_cue: str
    count_cue: str
    hard_fails: List[str]
    soft_warnings: List[str]


class ExerciseListResponse(BaseModel):
    items: List[Exercise]


class ExerciseRuleListResponse(BaseModel):
    items: List[ExerciseRuleConfig]


class TrainingPlan(BaseModel):
    id: int
    name: str
    goal_type: str
    level: str
    description: str
    schedule: List[str]


class TrainingPlanListResponse(BaseModel):
    items: List[TrainingPlan]


class UserProfile(BaseModel):
    id: int
    phone: str
    nickname: str
    gender: Optional[str] = None
    height_cm: Optional[int] = None
    weight_kg: Optional[int] = None
    fitness_goal: Optional[str] = None
    training_level: Optional[str] = None
    current_plan_id: Optional[int] = None


class SendOtpRequest(BaseModel):
    phone: str


class SendOtpResponse(BaseModel):
    success: bool
    dev_code: Optional[str] = None


class LoginRequest(BaseModel):
    phone: str
    code: str


class LoginResponse(BaseModel):
    token: str
    user: UserProfile


class UpdateProfileRequest(BaseModel):
    nickname: str
    gender: Optional[str] = None
    height_cm: Optional[int] = None
    weight_kg: Optional[int] = None
    fitness_goal: Optional[str] = None
    training_level: Optional[str] = None


class SelectPlanRequest(BaseModel):
    training_plan_id: int


class WorkoutSessionCreate(BaseModel):
    exercise_id: int
    started_at: datetime


class WorkoutSessionFinish(BaseModel):
    total_reps: int = Field(ge=0)
    avg_score: int = Field(ge=0, le=100)
    error_tags: List[str] = Field(default_factory=list)
    summary_text: str
    ended_at: datetime


class WorkoutHistoryItem(BaseModel):
    id: int
    exercise_name: str
    started_at: datetime
    ended_at: datetime
    total_reps: int
    avg_score: int
    error_tags: List[str]
    summary_text: str


class WorkoutHistoryResponse(BaseModel):
    items: List[WorkoutHistoryItem]


class TrendPoint(BaseModel):
    date: str
    sessions: int
    avg_score: int


class WorkoutTrendResponse(BaseModel):
    items: List[TrendPoint]


class SessionCreatedResponse(BaseModel):
    session_id: int


class FeedbackRequest(BaseModel):
    exercise_name: str
    avg_score: int
    error_tags: List[str]
    focus: Optional[str] = None


class FeedbackResponse(BaseModel):
    summary: str
    top_errors: List[str]
    next_time_focus: str
    coach_tip: str
