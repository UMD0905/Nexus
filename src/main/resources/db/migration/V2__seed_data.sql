-- =============================================================================
-- V2__seed_data.sql  – Default categories, tags, and sample tasks
-- Only runs on a fresh database (Flyway tracks migration history).
-- =============================================================================

-- ── Life areas ───────────────────────────────────────────────────────────────
INSERT INTO categories (name, color, icon, position) VALUES
  ('Work',          '#3B82F6', 'mdi2b-briefcase-outline',  1),
  ('Side Projects', '#8B5CF6', 'mdi2r-rocket-launch',      2),
  ('Kickboxing',    '#EF4444', 'mdi2b-boxing-glove',       3),
  ('Gym',           '#10B981', 'mdi2d-dumbbell',           4),
  ('Personal',      '#F59E0B', 'mdi2a-account-outline',    5);

-- ── Default tags ─────────────────────────────────────────────────────────────
INSERT INTO tags (name, color) VALUES
  ('urgent',   '#EF4444'),
  ('focus',    '#3B82F6'),
  ('quick',    '#10B981'),
  ('meeting',  '#8B5CF6'),
  ('health',   '#14B8A6');

-- ── Recurrence rules ─────────────────────────────────────────────────────────
-- Rule 1: Kickboxing — every Tuesday & Thursday
INSERT INTO recurrence_rules (type, days_of_week, interval_val)
  VALUES ('WEEKLY', 'TUE,THU', 1);

-- Rule 2: Gym — every Monday, Wednesday, Friday
INSERT INTO recurrence_rules (type, days_of_week, interval_val)
  VALUES ('WEEKLY', 'MON,WED,FRI', 1);

-- ── Sample project ───────────────────────────────────────────────────────────
INSERT INTO projects (name, description, category_id, status)
  VALUES ('Side Project v1', 'Get the MVP shipped and in front of users', 2, 'ACTIVE');

-- ── Sample tasks ─────────────────────────────────────────────────────────────
-- Kickboxing session (recurring)
INSERT INTO tasks (title, category_id, priority, status, is_urgent, is_important,
                   due_date, estimated_minutes, recurrence_rule_id, reminder_minutes_before)
VALUES ('Kickboxing training session', 3, 'HIGH', 'TODO', TRUE, TRUE,
        DATEADD('DAY', 1, CURRENT_TIMESTAMP), 90, 1, 30);

-- Gym session (recurring)
INSERT INTO tasks (title, category_id, priority, status, is_important,
                   due_date, estimated_minutes, recurrence_rule_id)
VALUES ('Gym – push day', 4, 'MEDIUM', 'TODO', TRUE,
        DATEADD('DAY', 1, CURRENT_TIMESTAMP), 75, 2);

-- Side project task
INSERT INTO tasks (title, category_id, project_id, priority, status,
                   is_urgent, is_important, due_date, estimated_minutes)
VALUES ('Build the landing page', 2, 1, 'HIGH', 'TODO', TRUE, TRUE,
        DATEADD('DAY', 3, CURRENT_TIMESTAMP), 120);

-- Work task
INSERT INTO tasks (title, category_id, priority, status, is_important,
                   due_date, estimated_minutes)
VALUES ('Weekly work review & planning', 1, 'MEDIUM', 'TODO', TRUE,
        DATEADD('DAY', 5, CURRENT_TIMESTAMP), 30);

-- Personal quick task
INSERT INTO tasks (title, category_id, priority, status,
                   due_date, estimated_minutes)
VALUES ('Book physio appointment', 5, 'LOW', 'TODO',
        DATEADD('DAY', 7, CURRENT_TIMESTAMP), 10);

-- A completed (done) task to show archive functionality
INSERT INTO tasks (title, category_id, priority, status,
                   due_date, estimated_minutes, completed_at, is_archived, archived_at)
VALUES ('Set up project repository', 2, 'MEDIUM', 'DONE',
        DATEADD('DAY', -3, CURRENT_TIMESTAMP), 30,
        DATEADD('DAY', -3, CURRENT_TIMESTAMP), TRUE,
        DATEADD('DAY', -3, CURRENT_TIMESTAMP));

-- ── Subtasks for the landing-page task ───────────────────────────────────────
INSERT INTO subtasks (task_id, title, position) VALUES
  (3, 'Write hero section copy',     0),
  (3, 'Choose colour palette',       1),
  (3, 'Add call-to-action button',   2),
  (3, 'Test on mobile',              3);

-- ── Tag the landing-page task as "focus" ─────────────────────────────────────
INSERT INTO task_tags (task_id, tag_id) VALUES (3, 2);  -- focus
INSERT INTO task_tags (task_id, tag_id) VALUES (1, 1);  -- kickboxing → urgent
