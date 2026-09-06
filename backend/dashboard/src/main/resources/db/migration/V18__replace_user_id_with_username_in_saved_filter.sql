ALTER TABLE saved_filter
    ADD COLUMN username VARCHAR(50);

UPDATE saved_filter sf
SET username = au.username FROM app_user au
WHERE sf.user_id = au.id;

ALTER TABLE saved_filter
    ALTER COLUMN username SET NOT NULL;

ALTER TABLE saved_filter DROP CONSTRAINT IF EXISTS saved_filter_user_id_fkey;
ALTER TABLE saved_filter DROP CONSTRAINT IF EXISTS saved_filter_user_id_name_key;

ALTER TABLE saved_filter DROP COLUMN user_id;

ALTER TABLE saved_filter
    ADD CONSTRAINT ux_saved_filter_user_name UNIQUE (username, name);