CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE exercise (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(1000),
    muscle_group VARCHAR(80),
    equipment VARCHAR(80),
    movement_type VARCHAR(40),
    is_compound BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_exercise_name ON exercise(name);

CREATE TABLE workout (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id),
    workout_date DATE NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    name VARCHAR(120),
    notes VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_workout_times CHECK (completed_at IS NULL OR started_at IS NULL OR completed_at >= started_at)
);
CREATE INDEX idx_workout_user_date ON workout(user_id, workout_date);

CREATE TABLE exercise_session (
    id UUID PRIMARY KEY,
    workout_id UUID NOT NULL REFERENCES workout(id) ON DELETE CASCADE,
    exercise_id UUID NOT NULL REFERENCES exercise(id),
    exercise_order INTEGER NOT NULL CHECK (exercise_order > 0),
    notes VARCHAR(1000),
    CONSTRAINT uk_session_workout_order UNIQUE (workout_id, exercise_order)
);
CREATE INDEX idx_session_exercise_workout ON exercise_session(exercise_id, workout_id);

CREATE TABLE workout_set (
    id UUID PRIMARY KEY,
    exercise_session_id UUID NOT NULL REFERENCES exercise_session(id) ON DELETE CASCADE,
    set_number INTEGER NOT NULL CHECK (set_number > 0),
    weight NUMERIC(8,2) NOT NULL CHECK (weight >= 0),
    reps INTEGER NOT NULL CHECK (reps > 0),
    rpe NUMERIC(4,2) CHECK (rpe >= 0 AND rpe <= 10),
    rir NUMERIC(4,2) CHECK (rir >= 0),
    is_warmup BOOLEAN NOT NULL,
    is_failure BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_set_session_number UNIQUE (exercise_session_id, set_number)
);
CREATE INDEX idx_set_session ON workout_set(exercise_session_id, set_number);

CREATE TABLE personal_record (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id),
    exercise_id UUID NOT NULL REFERENCES exercise(id),
    workout_set_id UUID NOT NULL REFERENCES workout_set(id) ON DELETE CASCADE,
    type VARCHAR(24) NOT NULL CHECK (type IN ('MAX_WEIGHT', 'MAX_REPS', 'ESTIMATED_ONE_REP_MAX', 'VOLUME')),
    value NUMERIC(12,4) NOT NULL,
    achieved_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_pr_user_exercise_date ON personal_record(user_id, exercise_id, achieved_at);
