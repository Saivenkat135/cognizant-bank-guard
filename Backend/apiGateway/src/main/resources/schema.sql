CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    is_approved BOOLEAN NOT NULL DEFAULT FALSE
);

-- Adds the column for installs that pre-date it. Errors here are ignored
-- (continueOnError=true in DatabaseConfig) — on a fresh DB the column is
-- already present from CREATE TABLE above.
ALTER TABLE users ADD COLUMN is_approved BOOLEAN NOT NULL DEFAULT FALSE;

-- SuperAdmins are seeded directly and never need approval.
UPDATE users SET is_approved = TRUE WHERE role = 'SUPER_ADMIN';
