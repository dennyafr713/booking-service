CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ---------------------------------------------------------------------
-- tabel users
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL    PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at    TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role  CHECK (role IN ('ADMIN', 'USER'))
);

-- ---------------------------------------------------------------------
-- tabel rooms
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rooms (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    capacity   INT          NOT NULL,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT ck_rooms_capacity CHECK (capacity > 0)
);

CREATE INDEX IF NOT EXISTS idx_rooms_is_active ON rooms (is_active);

-- ---------------------------------------------------------------------
-- tabel bookings
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS bookings (
    id         BIGSERIAL    PRIMARY KEY,
    room_id    BIGINT       NOT NULL,
    user_id    BIGINT       NOT NULL,
    title      VARCHAR(150) NOT NULL,
    start_time TIMESTAMP    NOT NULL,
    end_time   TIMESTAMP    NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'CONFIRMED',
    created_at TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT fk_bookings_room   FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_bookings_user   FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_bookings_status CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    CONSTRAINT ck_bookings_time   CHECK (end_time > start_time),
    CONSTRAINT ex_bookings_no_overlap EXCLUDE USING gist (
        room_id WITH =,
        tsrange(start_time, end_time, '[)') WITH &&
    ) WHERE (status = 'CONFIRMED')
);

CREATE INDEX IF NOT EXISTS idx_bookings_room_time_confirmed
    ON bookings (room_id, start_time, end_time)
    WHERE status = 'CONFIRMED';
CREATE INDEX IF NOT EXISTS idx_bookings_user_id ON bookings (user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_status_start ON bookings (status, start_time);

-- ---------------------------------------------------------------------
-- Seed data (password untuk semua akun: password123)
-- ---------------------------------------------------------------------
INSERT INTO users (name, email, password_hash, role) VALUES
    ('Administrator', 'admin@test.com', '$2a$10$Rc2d2qkdZTMmfm45nZS0wOymG5aZD0bEbDYZ2L6H9crxnx4MXfYwa', 'ADMIN'),
    ('User Satu',     'user1@test.com', '$2a$10$Rc2d2qkdZTMmfm45nZS0wOymG5aZD0bEbDYZ2L6H9crxnx4MXfYwa', 'USER'),
    ('User Dua',      'user2@test.com', '$2a$10$Rc2d2qkdZTMmfm45nZS0wOymG5aZD0bEbDYZ2L6H9crxnx4MXfYwa', 'USER')
ON CONFLICT (email) DO NOTHING;