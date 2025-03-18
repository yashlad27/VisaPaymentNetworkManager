-- Drop tables if they exist (for re-execution)
DROP TABLE IF EXISTS Authorization_Response, Authorization, Settlement, Transaction, Interchange_Fee, Exchange, Merchant, Acquiring_Bank, Issuing_Bank, Card, CardHolder;

-- CardHolder (Strong Entity)
CREATE TABLE CardHolder (
    cardholder_id INT PRIMARY KEY AUTO_INCREMENT,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(15) UNIQUE NOT NULL,
    address TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Card (Strong Entity)
CREATE TABLE Card (
    card_id INT PRIMARY KEY AUTO_INCREMENT,
    card_number_hash VARCHAR(64) UNIQUE NOT NULL,
    card_token VARCHAR(64) UNIQUE NOT NULL,
    card_type ENUM('Debit', 'Credit', 'Prepaid') NOT NULL,
    expiry_date DATE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    cardholder_id INT NOT NULL,
    FOREIGN KEY (cardholder_id) REFERENCES CardHolder(cardholder_id) ON DELETE CASCADE
);

-- Acquiring Bank (Strong Entity)
CREATE TABLE Acquiring_Bank (
    acquiring_bank_id INT PRIMARY KEY AUTO_INCREMENT,
    bank_name VARCHAR(255) NOT NULL,
    bank_code VARCHAR(10) UNIQUE NOT NULL,
    settlement_account VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Issuing Bank (Strong Entity)
CREATE TABLE Issuing_Bank (
    issuing_bank_id INT PRIMARY KEY AUTO_INCREMENT,
    bank_name VARCHAR(255) NOT NULL,
    bank_code VARCHAR(10) UNIQUE NOT NULL,
    is_active BOOLEAN DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Merchant (Strong Entity)
CREATE TABLE Merchant (
    merchant_id INT PRIMARY KEY AUTO_INCREMENT,
    merchant_name VARCHAR(255) NOT NULL,
    merchant_category_code VARCHAR(10) NOT NULL,
    terminal_id VARCHAR(20) UNIQUE NOT NULL,
    processing_fee DECIMAL(10,2) NOT NULL,
    settlement_amount DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Exchange (Strong Entity for Interchange Fee Management)
CREATE TABLE Exchange (
    exchange_id INT PRIMARY KEY AUTO_INCREMENT,
    exchange_name VARCHAR(255) NOT NULL
);

-- Interchange Fee (Weak Entity Dependent on Exchange)
CREATE TABLE Interchange_Fee (
    fee_id INT PRIMARY KEY AUTO_INCREMENT,
    exchange_id INT NOT NULL,
    card_type ENUM('Debit', 'Credit', 'Prepaid'),
    merchant_category VARCHAR(255) NOT NULL,
    percentage_fee DECIMAL(5,2) NOT NULL,
    fixed_fee DECIMAL(10,2) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (exchange_id) REFERENCES Exchange(exchange_id) ON DELETE CASCADE
);

-- Transaction (Strong Entity)
CREATE TABLE Transaction (
    transaction_id INT PRIMARY KEY AUTO_INCREMENT,
    amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(10) NOT NULL,
    transaction_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reference_number VARCHAR(100) UNIQUE NOT NULL,
    status ENUM('pending', 'completed', 'failed') NOT NULL,
    card_id INT NOT NULL,
    merchant_id INT NOT NULL,
    acquiring_bank_id INT NOT NULL,
    issuing_bank_id INT NOT NULL,
    FOREIGN KEY (card_id) REFERENCES Card(card_id) ON DELETE CASCADE,
    FOREIGN KEY (merchant_id) REFERENCES Merchant(merchant_id) ON DELETE CASCADE,
    FOREIGN KEY (acquiring_bank_id) REFERENCES Acquiring_Bank(acquiring_bank_id) ON DELETE CASCADE,
    FOREIGN KEY (issuing_bank_id) REFERENCES Issuing_Bank(issuing_bank_id) ON DELETE CASCADE
);

-- Authorization (Strong Entity)
CREATE TABLE Authorization (
    authorization_id INT PRIMARY KEY AUTO_INCREMENT,
    auth_code VARCHAR(50) UNIQUE NOT NULL,
    request_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    request_payload TEXT NOT NULL,
    status ENUM('approved', 'declined', 'pending') NOT NULL,
    transaction_id INT NOT NULL,
    FOREIGN KEY (transaction_id) REFERENCES Transaction(transaction_id) ON DELETE CASCADE
);

-- Authorization Response (Dependent on Authorization)
CREATE TABLE Authorization_Response (
    response_id INT PRIMARY KEY AUTO_INCREMENT,
    response_code VARCHAR(10) NOT NULL,
    response_message TEXT NOT NULL,
    response_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    response_payload TEXT NOT NULL,
    authorization_id INT NOT NULL,
    FOREIGN KEY (authorization_id) REFERENCES Authorization(authorization_id) ON DELETE CASCADE
);

-- Settlement (Strong Entity)
CREATE TABLE Settlement (
    settlement_id INT PRIMARY KEY AUTO_INCREMENT,
    total_amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status ENUM('pending', 'completed') NOT NULL,
    settlement_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    acquiring_bank_id INT NOT NULL,
    merchant_id INT NOT NULL,
    FOREIGN KEY (acquiring_bank_id) REFERENCES Acquiring_Bank(acquiring_bank_id) ON DELETE CASCADE,
    FOREIGN KEY (merchant_id) REFERENCES Merchant(merchant_id) ON DELETE CASCADE
);