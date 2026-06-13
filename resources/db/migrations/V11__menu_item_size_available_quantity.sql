ALTER TABLE menu_item_size
    ADD COLUMN IF NOT EXISTS available_quantity INTEGER NOT NULL DEFAULT -1;
