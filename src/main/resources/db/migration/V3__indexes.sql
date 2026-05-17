-- =============================================================================
-- V3__indexes.sql  – Performance indexes
-- =============================================================================

-- Tasks — most common query patterns
CREATE INDEX idx_tasks_category    ON tasks(category_id);
CREATE INDEX idx_tasks_project     ON tasks(project_id);
CREATE INDEX idx_tasks_due_date    ON tasks(due_date);
CREATE INDEX idx_tasks_status      ON tasks(status);
CREATE INDEX idx_tasks_archived    ON tasks(is_archived);
CREATE INDEX idx_tasks_parent      ON tasks(parent_task_id);

-- Time blocks — always queried by date
CREATE INDEX idx_time_blocks_date  ON time_blocks(block_date);

-- Pomodoro — queried by task and by date range (dashboard)
CREATE INDEX idx_pomodoro_task     ON pomodoro_sessions(task_id);
CREATE INDEX idx_pomodoro_started  ON pomodoro_sessions(started_at);

-- Notifications — unread badge count is frequent
CREATE INDEX idx_notif_unread      ON notifications(is_read);
CREATE INDEX idx_notif_task        ON notifications(task_id);
