-- =============================================================================
-- V5__streaks.sql  – Streak tracking table
-- One row per recurrence rule; updated each time a recurring task is done.
-- =============================================================================

CREATE TABLE streaks (
    id                  BIGINT       AUTO_INCREMENT PRIMARY KEY,
    recurrence_rule_id  BIGINT       REFERENCES recurrence_rules(id) ON DELETE CASCADE,
    title               VARCHAR(200) NOT NULL,   -- display name, e.g. "Kickboxing"
    category_id         BIGINT       REFERENCES categories(id) ON DELETE SET NULL,
    current_streak      INT          NOT NULL DEFAULT 0,
    longest_streak      INT          NOT NULL DEFAULT 0,
    last_completed_date DATE,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seed streaks for the two seeded recurrence rules
INSERT INTO streaks (recurrence_rule_id, title, category_id, current_streak, longest_streak)
  VALUES (1, 'Kickboxing', 3, 0, 0);   -- rule 1 = TUE,THU; category 3 = Kickboxing
INSERT INTO streaks (recurrence_rule_id, title, category_id, current_streak, longest_streak)
  VALUES (2, 'Gym', 4, 0, 0);           -- rule 2 = MON,WED,FRI; category 4 = Gym
