-- V13: task templates and energy/recovery journal

-- ── task_templates ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS task_templates (
    id                  BIGINT        AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(200)  NOT NULL,
    title               VARCHAR(500)  NOT NULL,
    description         TEXT,
    category_id         BIGINT        REFERENCES categories(id) ON DELETE SET NULL,
    project_id          BIGINT        REFERENCES projects(id)   ON DELETE SET NULL,
    priority            VARCHAR(10)   NOT NULL DEFAULT 'MEDIUM',
    estimated_minutes   INT,
    is_important        BOOLEAN       NOT NULL DEFAULT FALSE,
    is_urgent           BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ── template_subtasks ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS template_subtasks (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT       NOT NULL REFERENCES task_templates(id) ON DELETE CASCADE,
    title       VARCHAR(500) NOT NULL,
    position    INT          NOT NULL DEFAULT 0
);

-- ── energy_log ───────────────────────────────────────────────────────────────
-- Daily wellness journal entry; one row per calendar date
CREATE TABLE IF NOT EXISTS energy_log (
    id          BIGINT   AUTO_INCREMENT PRIMARY KEY,
    log_date    DATE     NOT NULL UNIQUE,
    sleep_hours DECIMAL(4,1),        -- e.g. 7.5
    energy      TINYINT,             -- 1–5
    soreness    TINYINT,             -- 1–5
    notes       TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ── tasks.required_energy ─────────────────────────────────────────────────────
-- LOW | MED | HIGH — used by Today view to warn when energy logged is insufficient
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS required_energy VARCHAR(5) DEFAULT NULL;
