CREATE TABLE addresses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    title VARCHAR(50),
    province VARCHAR(50),
    city VARCHAR(50),
    street VARCHAR(255),
    postal_code VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_addresses_user ON addresses(user_id);