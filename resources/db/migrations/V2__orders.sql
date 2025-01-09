DROP TABLE IF EXISTS user_order;
DROP TABLE IF EXISTS order_item;

-- Create 'order' table
CREATE TABLE "order"
(
    id      UUID NOT NULL PRIMARY KEY,
    note    TEXT NOT NULL,
    user_id TEXT REFERENCES account (id),
    status  TEXT
);

-- Create 'flavor_amount' table
CREATE TABLE order_item
(
    id          UUID NOT NULL PRIMARY KEY,
    order_id    UUID NOT NULL REFERENCES "order" (id) ON DELETE CASCADE,
    recipe_id   UUID NOT NULL REFERENCES recipe (id) ON DELETE CASCADE,
    amount      REAL NOT NULL,
    amount_unit TEXT NOT NULL,
    status      TEXT NOT NULL
);
