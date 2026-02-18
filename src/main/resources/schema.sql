CREATE TABLE IF NOT EXISTS blueprint (
    author VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    points JSONB NOT NULL,
    PRIMARY KEY (author, name)
);