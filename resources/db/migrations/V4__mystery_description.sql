ALTER TABLE recipe
ADD COLUMN is_mystery BOOLEAN DEFAULT FALSE,
ADD COLUMN description TEXT DEFAULT '',
ADD COLUMN mystery_description TEXT DEFAULT '';