CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(40) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    password_salt VARCHAR(64) NOT NULL,
    full_name VARCHAR(80) NOT NULL,
    phone VARCHAR(30),
    email VARCHAR(120),
    role VARCHAR(20) NOT NULL,
    card_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(60) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS books (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    isbn VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(160) NOT NULL,
    author VARCHAR(100) NOT NULL,
    publisher VARCHAR(120) NOT NULL,
    category_id BIGINT,
    total_copies INT NOT NULL,
    available_copies INT NOT NULL,
    location VARCHAR(40) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_book_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT ck_book_stock CHECK (total_copies >= 0 AND available_copies >= 0 AND available_copies <= total_copies)
);

CREATE TABLE IF NOT EXISTS loans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    borrowed_at TIMESTAMP NOT NULL,
    due_at TIMESTAMP NOT NULL,
    returned_at TIMESTAMP,
    renew_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT fk_loan_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_loan_book FOREIGN KEY (book_id) REFERENCES books(id)
);

CREATE TABLE IF NOT EXISTS reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    reserved_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    notified BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_reservation_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_reservation_book FOREIGN KEY (book_id) REFERENCES books(id)
);

CREATE TABLE IF NOT EXISTS fines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    reason VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL,
    paid_at TIMESTAMP,
    CONSTRAINT fk_fine_loan FOREIGN KEY (loan_id) REFERENCES loans(id),
    CONSTRAINT fk_fine_user FOREIGN KEY (user_id) REFERENCES users(id)
);
