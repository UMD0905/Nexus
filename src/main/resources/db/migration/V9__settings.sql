CREATE TABLE IF NOT EXISTS APP_SETTINGS (
    setting_key   VARCHAR(100) PRIMARY KEY,
    setting_value VARCHAR(2000) NOT NULL,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Default values
INSERT INTO APP_SETTINGS (setting_key, setting_value) VALUES
    ('default_priority',      'MEDIUM'),
    ('default_reminder_min',  '30'),
    ('pomodoro_work_min',     '25'),
    ('pomodoro_short_min',    '5'),
    ('pomodoro_long_min',     '15'),
    ('week_start_day',        'MONDAY'),
    ('auto_backup_enabled',   'false'),
    ('auto_backup_time',      '02:00'),
    ('auto_backup_dir',       '');
