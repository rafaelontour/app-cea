-- CEA SQLite database schema
-- Source: app/src/main/java/br/com/cea/data/CeaDatabaseHelper.kt
-- Database name: cea.db
-- Database version: 7
--
-- Notes:
-- - The current Android schema does not declare foreign key constraints.
-- - Mock/seed data is intentionally not included here.

CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT,
    age INTEGER,
    weight_kg REAL,
    height_cm REAL,
    activity_level TEXT,
    level TEXT,
    objective TEXT,
    frequency_per_week INTEGER,
    hours_per_day REAL,
    public_profile INTEGER
);

CREATE TABLE workouts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    title TEXT,
    objective TEXT,
    level TEXT,
    duration TEXT,
    is_public INTEGER DEFAULT 0,
    is_imported INTEGER DEFAULT 0,
    origin_workout_id INTEGER,
    origin_user_name TEXT
);

CREATE TABLE workout_exercises (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    workout_id INTEGER,
    exercise_name TEXT,
    sets INTEGER,
    reps TEXT,
    duration_seconds INTEGER,
    rest_seconds INTEGER,
    order_index INTEGER
);

CREATE TABLE exercise_catalog (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT,
    muscle_group TEXT,
    level TEXT,
    instructions TEXT,
    image_uri TEXT,
    primary_muscles TEXT,
    secondary_muscles TEXT,
    equipment TEXT
);

CREATE TABLE workout_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    workout_id INTEGER,
    completed_at INTEGER,
    duration_seconds INTEGER,
    notes TEXT
);

CREATE TABLE weekly_goals (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    week_start INTEGER,
    target_sessions INTEGER,
    completed_sessions INTEGER
);

CREATE TABLE achievements (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    code TEXT,
    title TEXT,
    earned_at INTEGER
);

CREATE TABLE weight_bmi_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    weight_kg REAL,
    height_cm REAL,
    bmi REAL,
    classification TEXT,
    recorded_at INTEGER
);

CREATE TABLE hydration_goals (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    daily_goal_ml INTEGER,
    reminder_enabled INTEGER,
    reminder_interval_minutes INTEGER
);

CREATE TABLE water_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    amount_ml INTEGER,
    logged_at INTEGER
);

CREATE TABLE app_preferences (
    key TEXT PRIMARY KEY,
    value TEXT
);

