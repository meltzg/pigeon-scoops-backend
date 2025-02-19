-- Add a "deleted" column to the 'recipe' table
ALTER TABLE recipe
ADD COLUMN deleted BOOLEAN DEFAULT FALSE;

-- Add a "deleted" column to the 'grocery' table
ALTER TABLE grocery
ADD COLUMN deleted BOOLEAN DEFAULT FALSE;

-- Add a "deleted" column to the 'user_order' table
ALTER TABLE user_order
ADD COLUMN deleted BOOLEAN DEFAULT FALSE;