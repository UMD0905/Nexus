-- Nothing structural needed: subtasks, tags, task_tags, projects tables already exist from V1-V5.
-- Add reminded_at column to avoid duplicate reminders
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS last_reminded_date DATE;
