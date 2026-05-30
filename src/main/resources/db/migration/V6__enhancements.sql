-- =============================================================================
-- V6__enhancements.sql  – Task time scheduling, recurring-task goal links,
--                         manual stat adjustments
-- =============================================================================

-- ── Task start time ───────────────────────────────────────────────────────────
-- Stores when the task is planned to begin (the existing due_date is the end/deadline).
ALTER TABLE tasks ADD COLUMN start_time TIME;

-- ── Manual stat adjustments ───────────────────────────────────────────────────
-- Stores per-key offsets that the user applies to the computed dashboard stats.
-- A reset clears all rows; recalculation then uses only live task data.
CREATE TABLE stat_adjustments (
    id         BIGINT      AUTO_INCREMENT PRIMARY KEY,
    stat_key   VARCHAR(50) NOT NULL UNIQUE,
    adjustment INT         NOT NULL DEFAULT 0,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);
