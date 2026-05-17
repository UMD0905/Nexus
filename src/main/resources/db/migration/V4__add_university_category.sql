-- =============================================================================
-- V4__add_university_category.sql
-- Adds the University life area for existing databases.
-- New databases already get all categories from V2 seed data,
-- so we only insert here if it doesn't exist yet.
-- =============================================================================
MERGE INTO categories (name, color, icon, position)
    KEY(name)
    VALUES ('University', '#06B6D4', 'mdi2s-school-outline', 6);
