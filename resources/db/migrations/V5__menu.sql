-- Drop existing tables to ensure a clean slate
DROP TABLE IF EXISTS menu;
DROP TABLE IF EXISTS menu_item;
DROP TABLE IF EXISTS menu_item_size;

CREATE TABLE menu
(
    id            UUID    NOT NULL PRIMARY KEY,
    name          TEXT    NOT NULL,
    repeats       BOOLEAN DEFAULT FALSE,
    active        BOOLEAN DEFAULT FALSE,
    duration      INTEGER NOT NULL,
    duration_type TEXT    NOT NULL,
    end_time      TIMESTAMPTZ
);

CREATE TABLE menu_item
(
    id        UUID NOT NULL PRIMARY KEY,
    menu_id   UUID NOT NULL REFERENCES menu (id) ON DELETE CASCADE,
    recipe_id UUID NOT NULL REFERENCES recipe (id) ON DELETE CASCADE
);

CREATE TABLE menu_item_size
(
    id           UUID NOT NULL PRIMARY KEY,
    menu_id   UUID NOT NULL REFERENCES menu (id) ON DELETE CASCADE,
    menu_item_id UUID NOT NULL REFERENCES menu_item (id) ON DELETE CASCADE,
    amount       REAL NOT NULL,
    amount_unit  TEXT NOT NULL
);
