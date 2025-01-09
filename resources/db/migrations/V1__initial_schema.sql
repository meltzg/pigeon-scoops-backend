-- Drop existing tables to ensure a clean slate
DROP TABLE IF EXISTS ingredient;
DROP TABLE IF EXISTS recipe;
DROP TABLE IF EXISTS grocery_unit;
DROP TABLE IF EXISTS grocery;
DROP TABLE IF EXISTS account;
DROP TABLE IF EXISTS recipe_favorite;

-- Create 'user' table
CREATE TABLE account
(
    id      TEXT NOT NULL PRIMARY KEY,
    name    TEXT NOT NULL,
    picture TEXT,
    UNIQUE (id)
);

-- Create 'grocery' table
CREATE TABLE grocery
(
    id         UUID NOT NULL PRIMARY KEY,
    name       TEXT NOT NULL,
    department TEXT NOT NULL
);

-- Create 'grocery_unit' table
CREATE TABLE grocery_unit
(
    id               UUID NOT NULL PRIMARY KEY,
    grocery_id       UUID NOT NULL REFERENCES grocery (id) ON DELETE CASCADE,
    source           TEXT NOT NULL,
    unit_cost        REAL NOT NULL,
    unit_mass        REAL,
    unit_mass_type   TEXT,
    unit_volume      REAL,
    unit_volume_type TEXT,
    unit_common      REAL,
    unit_common_type TEXT
);

-- Create 'recipe' table
CREATE TABLE recipe
(
    id           UUID    NOT NULL PRIMARY KEY,
    user_id      TEXT    NOT NULL REFERENCES account (id) ON DELETE CASCADE,
    name         TEXT    NOT NULL,
    instructions TEXT[] NOT NULL, -- Array of instructions as text
    amount       REAL    NOT NULL,
    amount_unit  TEXT    NOT NULL,
    source       TEXT,
    "public"     BOOLEAN NOT NULL,
    picture      TEXT
);

-- Create 'ingredient' table
CREATE TABLE ingredient
(
    id                    UUID NOT NULL PRIMARY KEY,
    recipe_id             UUID NOT NULL REFERENCES recipe (id) ON DELETE CASCADE,
    ingredient_grocery_id UUID REFERENCES grocery (id) ON DELETE CASCADE,
    ingredient_recipe_id  UUID REFERENCES recipe (id) ON DELETE CASCADE,
    amount                REAL NOT NULL,
    amount_unit           TEXT NOT NULL,
    CHECK (
        (ingredient_grocery_id IS NOT NULL AND ingredient_recipe_id IS NULL) OR
        (ingredient_grocery_id IS NULL AND ingredient_recipe_id IS NOT NULL)
        )
);

create table recipe_favorite
(
    recipe_id UUID NOT NULL REFERENCES recipe (id) ON DELETE CASCADE,
    user_id   TEXT NOT NULL REFERENCES account (id) ON DELETE CASCADE
);
