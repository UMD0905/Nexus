-- Many-to-many: a task or goal can belong to multiple life areas
CREATE TABLE task_categories (
    task_id     BIGINT NOT NULL REFERENCES tasks(id)      ON DELETE CASCADE,
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, category_id)
);

CREATE TABLE goal_categories (
    goal_id     BIGINT NOT NULL REFERENCES goals(id)      ON DELETE CASCADE,
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    PRIMARY KEY (goal_id, category_id)
);

-- Backfill existing single-category assignments so nothing is lost
INSERT INTO task_categories (task_id, category_id)
SELECT id, category_id FROM tasks WHERE category_id IS NOT NULL;

INSERT INTO goal_categories (goal_id, category_id)
SELECT id, category_id FROM goals WHERE category_id IS NOT NULL;
