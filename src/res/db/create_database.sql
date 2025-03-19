CREATE DATABASE visa_final_spring;

USE visa_final_spring;

CREATE TABLE Cardholder (
    cardholder_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20) UNIQUE
);

CREATE TABLE Card (
    card_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    card_num_hash VARCHAR(255) UNIQUE NOT NULL,
    card_type VARCHAR(20) NOT NULL,
    expiry_date DATE NOT NULL,
    is_active BOOLEAN NOT NULL,
    cardholder_id INT UNSIGNED NOT NULL, 
    FOREIGN KEY (cardholder_id) REFERENCES Cardholder(cardholder_id) 
        ON UPDATE CASCADE 
        ON DELETE CASCADE
);

CREATE TABLE IssuingBank (
    issuing_bank_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    bank_name VARCHAR(100) NOT NULL,
    bank_code VARCHAR(20) UNIQUE NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE Transaction (
    transaction_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    card_id INT UNSIGNED NOT NULL,
    FOREIGN KEY (card_id) REFERENCES Card(card_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

CREATE TABLE Authorization (
    auth_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    auth_code VARCHAR(50) UNIQUE NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    transaction_id INT UNSIGNED NOT NULL,
    FOREIGN KEY (transaction_id) REFERENCES Transaction(transaction_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE TABLE AuthResponse (
    response_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    response_code VARCHAR(10) NOT NULL,
    response_message TEXT NOT NULL,
    auth_id INT UNSIGNED NOT NULL,
    FOREIGN KEY (auth_id) REFERENCES Authorization(auth_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE TABLE Settlement (
    settlement_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    transaction_id INT UNSIGNED UNIQUE,
    FOREIGN KEY (transaction_id) REFERENCES Transaction(transaction_id)
        ON UPDATE CASCADE
        ON DELETE SET NULL
);

CREATE TABLE PaymentMerchant (
    merchant_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    merchant_name VARCHAR(100) NOT NULL,
    merchant_category VARCHAR(50),
    terminal_id VARCHAR(50) UNIQUE NOT NULL,
    processing_fee DECIMAL(5,2) NOT NULL
);

CREATE TABLE Exchange (
    exchange_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    exchange_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE AcquiringBank (
    acquiring_bank_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    bank_name VARCHAR(100) NOT NULL,
    bank_code VARCHAR(20) UNIQUE NOT NULL,
    settlement_account VARCHAR(50) UNIQUE NOT NULL,
    is_active BOOLEAN NOT NULL
);

CREATE TABLE InterchangeFee (
    fee_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    exchange_id INT UNSIGNED NOT NULL,
    card_type VARCHAR(20) NOT NULL,
    percentage_fee DECIMAL(5,2) NOT NULL,
    fixed_fee DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (exchange_id) REFERENCES Exchange(exchange_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

-- Relationships
ALTER TABLE Card ADD COLUMN issuing_bank_id INT UNSIGNED;
ALTER TABLE Card ADD FOREIGN KEY (issuing_bank_id) REFERENCES IssuingBank(issuing_bank_id)
    ON UPDATE CASCADE
    ON DELETE SET NULL;

ALTER TABLE Transaction ADD COLUMN merchant_id INT UNSIGNED;
ALTER TABLE Transaction ADD FOREIGN KEY (merchant_id) REFERENCES PaymentMerchant(merchant_id)
    ON UPDATE CASCADE
    ON DELETE SET NULL;

ALTER TABLE Transaction ADD COLUMN acquiring_bank_id INT UNSIGNED;
ALTER TABLE Transaction ADD FOREIGN KEY (acquiring_bank_id) REFERENCES AcquiringBank(acquiring_bank_id)
    ON UPDATE CASCADE
    ON DELETE SET NULL;

ALTER TABLE Transaction ADD COLUMN exchange_id INT UNSIGNED;
ALTER TABLE Transaction ADD FOREIGN KEY (exchange_id) REFERENCES Exchange(exchange_id)
    ON UPDATE CASCADE
    ON DELETE SET NULL;
