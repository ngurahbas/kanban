-- Flyway initial migration for Kanban application

-- Trigger function to update updated_at column
CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Boards table
CREATE TABLE IF NOT EXISTS kanban_board (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    columns VARCHAR(128)[] NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Cards table
CREATE TABLE IF NOT EXISTS kanban_card (
    board_id BIGINT NOT NULL,
    id INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    index INTEGER NOT NULL,
    "column" VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_kanban_card PRIMARY KEY (board_id, id),
    CONSTRAINT fk_kanban_card_board FOREIGN KEY (board_id) REFERENCES kanban_board (id) ON DELETE CASCADE
);

-- Triggers to maintain updated_at
DROP TRIGGER IF EXISTS set_timestamp_kanban_board ON kanban_board;
CREATE TRIGGER set_timestamp_kanban_board
BEFORE UPDATE ON kanban_board
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS set_timestamp_kanban_card ON kanban_card;
CREATE TRIGGER set_timestamp_kanban_card
BEFORE UPDATE ON kanban_card
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
