ALTER TABLE order_item
    ADD COLUMN IF NOT EXISTS menu_item_size_id UUID
    REFERENCES menu_item_size(id) ON DELETE SET NULL;