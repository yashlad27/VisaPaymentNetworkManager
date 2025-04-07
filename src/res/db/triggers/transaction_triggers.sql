-- Transaction Status Update Trigger
DELIMITER //
CREATE TRIGGER after_transaction_status_update
AFTER UPDATE ON Transaction
FOR EACH ROW
BEGIN
    IF NEW.status != OLD.status THEN
        INSERT INTO TransactionAuditLog (
            transaction_id,
            old_status,
            new_status,
            change_timestamp
        ) VALUES (
            NEW.transaction_id,
            OLD.status,
            NEW.status,
            NOW()
        );
    END IF;
END //
DELIMITER ;

-- Card Expiry Check Trigger
DELIMITER //
CREATE TRIGGER before_transaction_insert
BEFORE INSERT ON Transaction
FOR EACH ROW
BEGIN
    DECLARE card_expiry DATE;
    SELECT expiry_date INTO card_expiry
    FROM Card
    WHERE card_id = NEW.card_id;
    
    IF card_expiry < CURDATE() THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Card has expired';
    END IF;
END //
DELIMITER ; 