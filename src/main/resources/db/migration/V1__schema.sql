-- =============================================================================
-- V1__schema.sql  – Full Nexus schema
-- All tables created here; Flyway ensures this runs exactly once.
-- =============================================================================

-- ─────────────────────────── CATEGORIES (life areas) ─────────────────────────
CREATE TABLE categories (
    id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    color      VARCHAR(7)   NOT NULL DEFAULT '#4A90D9',   -- CSS hex colour
    icon       VARCHAR(100) NOT NULL DEFAULT 'mdi2b-briefcase',
    position   INT          NOT NULL DEFAULT 0,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ─────────────────────────────────── TAGS ────────────────────────────────────
CREATE TABLE tags (
    id    BIGINT      AUTO_INCREMENT PRIMARY KEY,
    name  VARCHAR(50) NOT NULL UNIQUE,
    color VARCHAR(7)  NOT NULL DEFAULT '#888888'
);

-- ──────────────────────────── RECURRENCE RULES ───────────────────────────────
-- Deliberately simple: DAILY / WEEKLY / WEEKDAYS only.
-- days_of_week is a comma-separated list: MON,TUE,WED,THU,FRI,SAT,SUN
CREATE TABLE recurrence_rules (
    id           BIGINT      AUTO_INCREMENT PRIMARY KEY,
    type         VARCHAR(20) NOT NULL,        -- DAILY | WEEKLY | WEEKDAYS
    days_of_week VARCHAR(50),                  -- e.g. "TUE,THU" (null → every occurrence)
    interval_val INT         NOT NULL DEFAULT 1,
    end_date     DATE                          -- null = repeat forever
);

-- ──────────────────────────────── PROJECTS ───────────────────────────────────
CREATE TABLE projects (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    category_id BIGINT       REFERENCES categories(id) ON DELETE SET NULL,
    color       VARCHAR(7)   NOT NULL DEFAULT '#4A90D9',
    start_date  DATE,
    due_date    DATE,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | COMPLETED | ARCHIVED
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ────────────────────────────────── TASKS ────────────────────────────────────
CREATE TABLE tasks (
    id                      BIGINT       AUTO_INCREMENT PRIMARY KEY,
    title                   VARCHAR(500) NOT NULL,
    description             TEXT,
    category_id             BIGINT       REFERENCES categories(id)        ON DELETE SET NULL,
    project_id              BIGINT       REFERENCES projects(id)          ON DELETE SET NULL,
    priority                VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM',  -- LOW|MEDIUM|HIGH|CRITICAL
    status                  VARCHAR(15)  NOT NULL DEFAULT 'TODO',    -- TODO|IN_PROGRESS|DONE|CANCELLED
    due_date                TIMESTAMP,
    estimated_minutes       INT,
    actual_minutes          INT          NOT NULL DEFAULT 0,  -- accumulated from pomodoro sessions
    recurrence_rule_id      BIGINT       REFERENCES recurrence_rules(id)  ON DELETE SET NULL,
    parent_task_id          BIGINT       REFERENCES tasks(id)             ON DELETE CASCADE,
    reminder_minutes_before INT,                              -- null = no reminder
    is_important            BOOLEAN      NOT NULL DEFAULT FALSE,  -- Eisenhower: important axis
    is_urgent               BOOLEAN      NOT NULL DEFAULT FALSE,  -- Eisenhower: urgent axis
    -- Archive columns (Phase 1 addition)
    completed_at            TIMESTAMP,                        -- stamped when status → DONE
    is_archived             BOOLEAN      NOT NULL DEFAULT FALSE,
    archived_at             TIMESTAMP,                        -- stamped when archived
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Task ↔ Tag  (many-to-many)
CREATE TABLE task_tags (
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    tag_id  BIGINT NOT NULL REFERENCES tags(id)  ON DELETE CASCADE,
    PRIMARY KEY (task_id, tag_id)
);

-- ────────────────────────────────── SUBTASKS ─────────────────────────────────
-- Checklist items belonging to a task.
CREATE TABLE subtasks (
    id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
    task_id    BIGINT       NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    title      VARCHAR(300) NOT NULL,
    completed  BOOLEAN      NOT NULL DEFAULT FALSE,
    position   INT          NOT NULL DEFAULT 0,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ─────────────────────────────────── GOALS ───────────────────────────────────
CREATE TABLE goals (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    category_id BIGINT       REFERENCES categories(id) ON DELETE SET NULL,
    target_date DATE,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | COMPLETED | ABANDONED
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Goal ↔ Task  (many-to-many: a goal is made up of tasks)
CREATE TABLE goal_tasks (
    goal_id BIGINT NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    PRIMARY KEY (goal_id, task_id)
);

-- ─────────────────────── TIME BLOCKS (day planner) ───────────────────────────
CREATE TABLE time_blocks (
    id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
    task_id    BIGINT       REFERENCES tasks(id) ON DELETE SET NULL,  -- null = free/buffer block
    title      VARCHAR(200),                                           -- label when no task linked
    block_date DATE         NOT NULL,
    start_time TIME         NOT NULL,
    end_time   TIME         NOT NULL,
    color      VARCHAR(7),
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ──────────────────────────── POMODORO SESSIONS ──────────────────────────────
CREATE TABLE pomodoro_sessions (
    id               BIGINT    AUTO_INCREMENT PRIMARY KEY,
    task_id          BIGINT    NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    started_at       TIMESTAMP NOT NULL,
    ended_at         TIMESTAMP,                     -- null if the session was interrupted
    duration_minutes INT       NOT NULL DEFAULT 25,
    completed        BOOLEAN   NOT NULL DEFAULT FALSE,
    notes            TEXT,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ──────────────────────────── IN-APP NOTIFICATIONS ───────────────────────────
CREATE TABLE notifications (
    id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
    title      VARCHAR(200) NOT NULL,
    message    TEXT,
    type       VARCHAR(20)  NOT NULL DEFAULT 'REMINDER',  -- REMINDER | SYSTEM | STREAK
    task_id    BIGINT       REFERENCES tasks(id) ON DELETE SET NULL,
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
