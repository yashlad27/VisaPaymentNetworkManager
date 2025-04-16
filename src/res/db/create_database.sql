CREATE
DATABASE visa_final_spring;

USE
visa_final_spring;

CREATE TABLE Cardholder
(
    cardholder_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    first_name    VARCHAR(50),
    last_name     VARCHAR(50),
    email         VARCHAR(100) UNIQUE,
    phone         VARCHAR(20) UNIQUE
);

CREATE TABLE Card
(
    card_id       INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    card_num_hash VARCHAR(255) UNIQUE NOT NULL,
    card_type     VARCHAR(20)         NOT NULL,
    expiry_date   DATE                NOT NULL,
    is_active     BOOLEAN             NOT NULL,
    cardholder_id INT UNSIGNED NOT NULL,
    FOREIGN KEY (cardholder_id) REFERENCES Cardholder (cardholder_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE TABLE IssuingBank
(
    issuing_bank_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    bank_name       VARCHAR(100)       NOT NULL,
    bank_code       VARCHAR(20) UNIQUE NOT NULL,
    is_active       BOOLEAN            NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE Transaction
(
    transaction_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    amount         DECIMAL(10, 2) NOT NULL,
    currency       VARCHAR(10)    NOT NULL,
    timestamp      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status         VARCHAR(20)    NOT NULL,
    card_id        INT UNSIGNED NOT NULL,
    FOREIGN KEY (card_id) REFERENCES Card (card_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

CREATE TABLE Authorization
(
    auth_id        INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    auth_code      VARCHAR(50) UNIQUE NOT NULL,
    timestamp      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status         VARCHAR(20)        NOT NULL,
    transaction_id INT UNSIGNED NOT NULL,
    FOREIGN KEY (transaction_id) REFERENCES Transaction (transaction_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE TABLE AuthResponse
(
    response_id      INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    response_code    VARCHAR(10) NOT NULL,
    response_message TEXT        NOT NULL,
    auth_id          INT UNSIGNED NOT NULL,
    FOREIGN KEY (auth_id) REFERENCES Authorization (auth_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE TABLE Settlement
(
    settlement_id  INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    transaction_id INT UNSIGNED UNIQUE,
    FOREIGN KEY (transaction_id) REFERENCES Transaction (transaction_id)
        ON UPDATE CASCADE
        ON DELETE SET NULL
);

CREATE TABLE PaymentMerchant
(
    merchant_id       INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    merchant_name     VARCHAR(100)       NOT NULL,
    merchant_category VARCHAR(50),
    terminal_id       VARCHAR(50) UNIQUE NOT NULL,
    processing_fee    DECIMAL(5, 2)      NOT NULL
);

CREATE TABLE Exchange
(
    exchange_id   INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    exchange_date DATE        NOT NULL,
    status        VARCHAR(20) NOT NULL
);

CREATE TABLE AcquiringBank
(
    acquiring_bank_id  INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    bank_name          VARCHAR(100)       NOT NULL,
    bank_code          VARCHAR(20) UNIQUE NOT NULL,
    settlement_account VARCHAR(50) UNIQUE NOT NULL,
    is_active          BOOLEAN            NOT NULL
);

CREATE TABLE InterchangeFee
(
    fee_id         INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    exchange_id    INT UNSIGNED NOT NULL,
    card_type      VARCHAR(20)    NOT NULL,
    percentage_fee DECIMAL(5, 2)  NOT NULL,
    fixed_fee      DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (exchange_id) REFERENCES Exchange (exchange_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

-- Relationships
ALTER TABLE Card
    ADD COLUMN issuing_bank_id INT UNSIGNED;
ALTER TABLE Card
    ADD FOREIGN KEY (issuing_bank_id) REFERENCES IssuingBank (issuing_bank_id)
        ON UPDATE CASCADE
        ON DELETE SET NULL;

ALTER TABLE Transaction
    ADD COLUMN merchant_id INT UNSIGNED;
ALTER TABLE Transaction
    ADD FOREIGN KEY (merchant_id) REFERENCES PaymentMerchant (merchant_id)
        ON UPDATE CASCADE
        ON DELETE SET NULL;

ALTER TABLE Transaction
    ADD COLUMN acquiring_bank_id INT UNSIGNED;
ALTER TABLE Transaction
    ADD FOREIGN KEY (acquiring_bank_id) REFERENCES AcquiringBank (acquiring_bank_id)
        ON UPDATE CASCADE
        ON DELETE SET NULL;

ALTER TABLE Transaction
    ADD COLUMN exchange_id INT UNSIGNED;
ALTER TABLE Transaction
    ADD FOREIGN KEY (exchange_id) REFERENCES Exchange (exchange_id)
        ON UPDATE CASCADE
        ON DELETE SET NULL;

DELIMITER
//

-- Trigger to automatically create an Authorization record when a transaction is created
CREATE TRIGGER after_transaction_insert
    AFTER INSERT
    ON Transaction
    FOR EACH ROW
BEGIN
    DECLARE auth_code_val VARCHAR(50);
    SET auth_code_val = CONCAT('AUTH', NEW.transaction_id, '-', FLOOR(RAND() * 1000000));
    INSERT INTO Authorization(auth_code, status, transaction_id)
    VALUES (auth_code_val, NEW.status, NEW.transaction_id);
END //

-- Trigger to create an audit log when a card status is changed
CREATE TABLE IF NOT EXISTS CardStatusLog
(
    log_id
    INT
    UNSIGNED
    AUTO_INCREMENT
    PRIMARY
    KEY,
    card_id
    INT
    UNSIGNED
    NOT
    NULL,
    old_status
    BOOLEAN,
    new_status
    BOOLEAN,
    changed_at
    TIMESTAMP
    DEFAULT
    CURRENT_TIMESTAMP
) //

-- create an audit log when a card's status changes:
CREATE TRIGGER after_card_status_update
    AFTER UPDATE
    ON Card
    FOR EACH ROW
BEGIN
    IF OLD.is_active != NEW.is_active THEN
        INSERT INTO CardStatusLog(card_id, old_status, new_status)
        VALUES(NEW.card_id, OLD.is_active, NEW.is_active);
END IF;
END
//

-- Trigger to update transaction status after authorization response
CREATE TRIGGER after_auth_response_insert
    AFTER INSERT
    ON AuthResponse
    FOR EACH ROW
BEGIN
    DECLARE auth_transaction_id INT UNSIGNED;
    DECLARE response_status VARCHAR(20);

    SELECT transaction_id
    INTO auth_transaction_id
    FROM Authorization
    WHERE auth_id = NEW.auth_id;

    IF NEW.response_code = '00' THEN
        SET response_status = 'Approved';
    ELSE
        SET response_status = 'Declined';
END IF;

UPDATE Transaction
SET status = response_status
WHERE transaction_id = auth_transaction_id;

UPDATE Authorization
SET status = response_status
WHERE auth_id = NEW.auth_id;
END
//

DELIMITER ;
