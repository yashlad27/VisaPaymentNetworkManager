-- Transaction Audit Log Table
CREATE TABLE TransactionAuditLog (
    audit_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    transaction_id INT UNSIGNED NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    change_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (transaction_id) REFERENCES Transaction(transaction_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

-- Archived Transactions Table
CREATE TABLE ArchivedTransactions (
    transaction_id INT UNSIGNED PRIMARY KEY,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    timestamp TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    card_id INT UNSIGNED,
    merchant_id INT UNSIGNED,
    acquiring_bank_id INT UNSIGNED,
    exchange_id INT UNSIGNED,
    archive_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bank Performance Report Table
CREATE TABLE BankPerformanceReport (
    report_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    bank_id INT UNSIGNED NOT NULL,
    total_transactions INT UNSIGNED NOT NULL,
    success_rate DECIMAL(5,2) NOT NULL,
    average_amount DECIMAL(10,2) NOT NULL,
    report_date DATE NOT NULL,
    FOREIGN KEY (bank_id) REFERENCES AcquiringBank(acquiring_bank_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
); 