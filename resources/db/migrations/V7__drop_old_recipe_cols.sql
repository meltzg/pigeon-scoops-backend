ALTER TABLE recipe
    DROP COLUMN IF EXISTS public,
    DROP COLUMN IF EXISTS user_id;

DROP TABLE IF EXISTS recipe_favorite;