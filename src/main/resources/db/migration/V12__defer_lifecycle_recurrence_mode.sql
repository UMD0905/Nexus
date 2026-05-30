-- V12: defer-until date, GTD lifecycle buckets, recurrence completion mode

-- ── tasks.defer_until ─────────────────────────────────────────────────────────
-- Tasks with a future defer_until are hidden from Today / Week / All Tasks.
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS defer_until TIMESTAMP DEFAULT NULL;

-- ── tasks.lifecycle ───────────────────────────────────────────────────────────
-- INBOX    = landed from quick-add, not yet processed
-- ANYTIME  = processed, no specific date (default)
-- TODAY    = explicitly scheduled for today
-- SOMEDAY  = indefinitely deferred (hidden from main views)
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS lifecycle VARCHAR(10) NOT NULL DEFAULT 'ANYTIME';

-- ── recurrence_rules.mode ─────────────────────────────────────────────────────
-- FIXED           = generate instances on the calendar regardless of completion
-- AFTER_COMPLETION = generate next instance only when the current one is marked done
ALTER TABLE recurrence_rules ADD COLUMN IF NOT EXISTS mode VARCHAR(20) NOT NULL DEFAULT 'FIXED';
