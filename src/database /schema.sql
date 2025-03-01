-- 1. Procedure to process settlements for a merchant
DELIMITER //
CREATE PROCEDURE process_merchant_settlement(
    IN p_merchant_id INT,
    IN p_settlement_date DATE,
    OUT p_total_amount DECIMAL(10,2),
    OUT p_transaction_count INT
)
BEGIN
    DECLARE v_done INT DEFAULT FALSE;
    DECLARE v_transaction_id INT;
    DECLARE v_amount DECIMAL(10,2);
    DECLARE v_current_total DECIMAL(10,2) DEFAULT 0;
    DECLARE v_count INT DEFAULT 0;
    DECLARE v_processing_fee DECIMAL(5,2);
    DECLARE v_error_message VARCHAR(255);
    
    -- Cursor for retrieving completed transactions that need settlement
    DECLARE transaction_cursor CURSOR FOR
        SELECT transaction_id, amount 
        FROM transactions
        WHERE merchant_id = p_merchant_id 
        AND transaction_status = 'completed'
        AND transaction_id NOT IN (
            SELECT t.transaction_id 
            FROM transactions t
            JOIN settlements s ON t.merchant_id = s.merchant_id
            WHERE s.settlement_date = p_settlement_date
        );
    
    -- Exit handler for cursor
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = TRUE;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        GET DIAGNOSTICS CONDITION 1
        v_error_message = MESSAGE_TEXT;
        
        -- Log the error
        INSERT INTO error_log(procedure_name, error_message, error_timestamp)
        VALUES ('process_merchant_settlement', v_error_message, NOW());
        
        -- Rollback any changes
        ROLLBACK;
        
        -- Resignal the error
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = v_error_message;
    END;
    
    -- Get merchant processing fee
    SELECT processing_fee INTO v_processing_fee
    FROM merchants
    WHERE merchant_id = p_merchant_id;
    
    -- Start transaction
    START TRANSACTION;
    
    -- Open cursor
    OPEN transaction_cursor;
    
    -- Process each transaction
    transaction_loop: LOOP
        FETCH transaction_cursor INTO v_transaction_id, v_amount;
        
        IF v_done THEN
            LEAVE transaction_loop;
        END IF;
        
        -- Update transaction status to 'settled'
        UPDATE transactions
        SET transaction_status = 'settled'
        WHERE transaction_id = v_transaction_id;
        
        -- Calculate settlement amount (subtracting processing fee)
        SET v_current_total = v_current_total + (v_amount - (v_amount * v_processing_fee / 100));
        SET v_count = v_count + 1;
    END LOOP;
    
    -- Close cursor
    CLOSE transaction_cursor;
    
    -- If we have transactions to settle, create a settlement record
    IF v_count > 0 THEN
        INSERT INTO settlements(merchant_id, total_amount, currency, status, settlement_date)
        VALUES(p_merchant_id, v_current_total, 'USD', 'completed', p_settlement_date);
        
        -- Update merchant's settlement amount
        UPDATE merchants
        SET settlement_amount = settlement_amount + v_current_total
        WHERE merchant_id = p_merchant_id;
    END IF;
    
    -- Commit transaction
    COMMIT;
    
    -- Set output parameters
    SET p_total_amount = v_current_total;
    SET p_transaction_count = v_count;
END //
DELIMITER ;

-- 2. Procedure to authorize a new transaction
DELIMITER //
CREATE PROCEDURE authorize_transaction(
    IN p_card_id INT,
    IN p_merchant_id INT,
    IN p_amount DECIMAL(10,2),
    IN p_reference_number VARCHAR(50),
    INOUT p_transaction_id INT,
    OUT p_authorization_status VARCHAR(20),
    OUT p_auth_code VARCHAR(20)
)
BEGIN
    DECLARE v_card_type VARCHAR(20);
    DECLARE v_expiry_date DATE;
    DECLARE v_is_card_active BOOLEAN;
    DECLARE v_merchant_category VARCHAR(20);
    DECLARE v_request_payload JSON;
    DECLARE v_status VARCHAR(20);
    DECLARE v_auth_id INT;
    DECLARE v_error_message VARCHAR(255);
    
    -- Exit handler for exceptions
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        GET DIAGNOSTICS CONDITION 1
        v_error_message = MESSAGE_TEXT;
        
        SET p_authorization_status = 'error';
        SET p_auth_code = NULL;
        SET p_transaction_id = NULL;
        
        -- Log the error
        INSERT INTO error_log(procedure_name, error_message, error_timestamp)
        VALUES ('authorize_transaction', v_error_message, NOW());
        
        -- Rollback any changes
        ROLLBACK;
    END;
    
    -- Start transaction
    START TRANSACTION;
    
    -- Get card details
    SELECT c.card_type, c.expiry_date, c.is_active, m.merchant_category_code
    INTO v_card_type, v_expiry_date, v_is_card_active, v_merchant_category
    FROM cards c
    JOIN merchants m ON m.merchant_id = p_merchant_id
    WHERE c.card_id = p_card_id;
    
    -- Create initial transaction record
    INSERT INTO transactions(card_id, merchant_id, amount, currency, reference_number, transaction_status)
    VALUES(p_card_id, p_merchant_id, p_amount, 'USD', p_reference_number, 'initiated');
    
    -- Get the transaction ID
    SET p_transaction_id = LAST_INSERT_ID();
    
    -- Prepare request payload
    SET v_request_payload = JSON_OBJECT(
        'transaction_id', p_transaction_id,
        'card_id', p_card_id,
        'merchant_id', p_merchant_id,
        'amount', p_amount,
        'reference_number', p_reference_number,
        'timestamp', NOW()
    );
    
    -- Generate auth code (simple example - in reality would be more complex)
    SET p_auth_code = CONCAT('AUTH', LPAD(p_transaction_id, 10, '0'));
    
    -- Check if card is valid for authorization
    IF v_is_card_active = TRUE AND v_expiry_date > CURDATE() THEN
        -- Insert authorization record
        INSERT INTO authorizations(transaction_id, auth_code, request_timestamp, request_payload, status)
        VALUES(p_transaction_id, p_auth_code, NOW(), v_request_payload, 'approved');
        
        SET v_auth_id = LAST_INSERT_ID();
        SET p_authorization_status = 'approved';
        SET v_status = 'authorized';
        
        -- Insert authorization response
        INSERT INTO authorization_responses(authorization_id, response_code, response_message, response_timestamp, response_payload)
        VALUES(v_auth_id, '00', 'Approved', NOW(), JSON_OBJECT(
            'status', 'approved',
            'auth_code', p_auth_code,
            'transaction_id', p_transaction_id
        ));
    ELSE
        -- Insert declined authorization record
        INSERT INTO authorizations(transaction_id, auth_code, request_timestamp, request_payload, status)
        VALUES(p_transaction_id, p_auth_code, NOW(), v_request_payload, 'declined');
        
        SET v_auth_id = LAST_INSERT_ID();
        SET p_authorization_status = 'declined';
        SET v_status = 'declined';
        
        -- Insert authorization response
        INSERT INTO authorization_responses(authorization_id, response_code, response_message, response_timestamp, response_payload)
        VALUES(
            v_auth_id, 
            CASE 
                WHEN NOT v_is_card_active THEN '14' 
                WHEN v_expiry_date <= CURDATE() THEN '54'
                ELSE '05'
            END, 
            CASE 
                WHEN NOT v_is_card_active THEN 'Invalid card' 
                WHEN v_expiry_date <= CURDATE() THEN 'Expired card'
                ELSE 'Do not honor'
            END, 
            NOW(), 
            JSON_OBJECT(
                'status', 'declined',
                'reason', CASE 
                    WHEN NOT v_is_card_active THEN 'Card inactive' 
                    WHEN v_expiry_date <= CURDATE() THEN 'Card expired'
                    ELSE 'General decline'
                END,
                'transaction_id', p_transaction_id
            )
        );
    END IF;
    
    -- Update transaction status
    UPDATE transactions
    SET transaction_status = v_status
    WHERE transaction_id = p_transaction_id;
    
    -- Commit transaction
    COMMIT;
END //
DELIMITER ;

-- 3. Procedure to generate transaction summary report
DELIMITER //
CREATE PROCEDURE generate_transaction_summary(
    IN p_start_date DATE,
    IN p_end_date DATE,
    IN p_grouping VARCHAR(20) -- 'daily', 'weekly', 'monthly'
)
BEGIN
    DECLARE v_date_format VARCHAR(20);
    
    -- Set date format based on grouping parameter
    CASE p_grouping
        WHEN 'daily' THEN SET v_date_format = '%Y-%m-%d';
        WHEN 'weekly' THEN SET v_date_format = '%Y-%u';
        WHEN 'monthly' THEN SET v_date_format = '%Y-%m';
        ELSE SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid grouping parameter. Use daily, weekly, or monthly.';
    END CASE;
    
    -- Return the summary based on grouping
    SELECT 
        DATE_FORMAT(transaction_timestamp, v_date_format) AS time_period,
        COUNT(*) AS transaction_count,
        SUM(amount) AS total_amount,
        AVG(amount) AS average_amount,
        COUNT(CASE WHEN transaction_status = 'completed' OR transaction_status = 'settled' THEN 1 END) AS successful_count,
        COUNT(CASE WHEN transaction_status = 'declined' THEN 1 END) AS declined_count,
        (COUNT(CASE WHEN transaction_status = 'completed' OR transaction_status = 'settled' THEN 1 END) / COUNT(*)) * 100 AS success_rate
    FROM transactions
    WHERE transaction_timestamp BETWEEN p_start_date AND p_end_date
    GROUP BY time_period
    ORDER BY time_period;
END //
DELIMITER ;

-- 4. Procedure to calculate interchange fees
DELIMITER //
CREATE PROCEDURE calculate_interchange_fees(
    IN p_transaction_id INT,
    OUT p_interchange_fee DECIMAL(10,2)
)
BEGIN
    DECLARE v_card_type VARCHAR(20);
    DECLARE v_merchant_category VARCHAR(20);
    DECLARE v_amount DECIMAL(10,2);
    DECLARE v_percentage_fee DECIMAL(5,2);
    DECLARE v_fixed_fee DECIMAL(5,2);
    
    -- Get transaction details
    SELECT c.card_type, m.merchant_category_code, t.amount
    INTO v_card_type, v_merchant_category, v_amount
    FROM transactions t
    JOIN cards c ON t.card_id = c.card_id
    JOIN merchants m ON t.merchant_id = m.merchant_id
    WHERE t.transaction_id = p_transaction_id;
    
    -- Get applicable interchange fee
    SELECT percentage_fee, fixed_fee
    INTO v_percentage_fee, v_fixed_fee
    FROM interchange_fees
    WHERE card_type = v_card_type
    AND merchant_category = v_merchant_category
    AND CURDATE() BETWEEN effective_from AND IFNULL(effective_to, '9999-12-31')
    ORDER BY effective_from DESC
    LIMIT 1;
    
    -- Calculate fee
    SET p_interchange_fee = (v_amount * v_percentage_fee / 100) + v_fixed_fee;
END //
DELIMITER ;

-- 5. Procedure to update card status in bulk
DELIMITER //
CREATE PROCEDURE bulk_update_card_status(
    IN p_expiry_month INT,
    IN p_expiry_year INT,
    IN p_new_status BOOLEAN,
    OUT p_cards_updated INT
)
BEGIN
    DECLARE v_done INT DEFAULT FALSE;
    DECLARE v_card_id INT;
    DECLARE v_expiry_date DATE;
    DECLARE v_count INT DEFAULT 0;
    
    -- Cursor for cards with matching expiry date
    DECLARE card_cursor CURSOR FOR
        SELECT card_id, expiry_date
        FROM cards
        WHERE MONTH(expiry_date) = p_expiry_month
        AND YEAR(expiry_date) = p_expiry_year;
    
    -- Handlers
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = TRUE;
    
    -- Open cursor
    OPEN card_cursor;
    
    -- Start a transaction
    START TRANSACTION;
    
    -- Process each card
    card_loop: LOOP
        FETCH card_cursor INTO v_card_id, v_expiry_date;
        
        IF v_done THEN
            LEAVE card_loop;
        END IF;
        
        -- Update card status
        UPDATE cards
        SET is_active = p_new_status
        WHERE card_id = v_card_id;
        
        -- Increment counter
        SET v_count = v_count + 1;
    END LOOP;
    
    -- Close cursor
    CLOSE card_cursor;
    
    -- Commit the transaction
    COMMIT;
    
    -- Set output parameter
    SET p_cards_updated = v_count;
END //
DELIMITER ;

-- 6. Procedure to find fraudulent transaction patterns
DELIMITER //
CREATE PROCEDURE detect_fraud_patterns(
    IN p_cardholder_id INT,
    IN p_time_window_hours INT,
    IN p_transaction_threshold INT,
    INOUT p_suspicious_card_list JSON
)
BEGIN
    DECLARE v_suspicious_count INT DEFAULT 0;
    DECLARE v_card_id INT;
    DECLARE v_transaction_count INT;
    DECLARE v_done INT DEFAULT FALSE;
    DECLARE v_suspicious_cards JSON DEFAULT JSON_ARRAY();
    
    -- Cursor for cards belonging to the cardholder
    DECLARE card_cursor CURSOR FOR
        SELECT card_id FROM cards WHERE cardholder_id = p_cardholder_id;
    
    -- Handler for cursor
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = TRUE;
    
    -- Open cursor
    OPEN card_cursor;
    
    -- Loop through each card
    card_loop: LOOP
        FETCH card_cursor INTO v_card_id;
        
        IF v_done THEN
            LEAVE card_loop;
        END IF;
        
        -- Count recent transactions for this card
        SELECT COUNT(*) INTO v_transaction_count
        FROM transactions
        WHERE card_id = v_card_id
        AND transaction_timestamp >= DATE_SUB(NOW(), INTERVAL p_time_window_hours HOUR);
        
        -- Check if transaction count exceeds threshold
        IF v_transaction_count >= p_transaction_threshold THEN
            -- Add to suspicious list
            SET v_suspicious_cards = JSON_ARRAY_APPEND(
                v_suspicious_cards, 
                '$', 
                JSON_OBJECT(
                    'card_id', v_card_id,
                    'transaction_count', v_transaction_count,
                    'time_window_hours', p_time_window_hours
                )
            );
            
            SET v_suspicious_count = v_suspicious_count + 1;
            
            -- Insert into fraud monitoring table (if it exists)
            -- This would be used for tracking purposes
            IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'fraud_monitoring') THEN
                INSERT INTO fraud_monitoring(card_id, detection_time, transaction_count, detection_method)
                VALUES(v_card_id, NOW(), v_transaction_count, 'High frequency pattern');
            END IF;
        END IF;
    END LOOP;
    
    -- Close cursor
    CLOSE card_cursor;
    
    -- Set output parameter
    SET p_suspicious_card_list = v_suspicious_cards;
    
    -- Return result with details
    SELECT 
        p_cardholder_id AS cardholder_id,
        v_suspicious_count AS suspicious_card_count,
        p_suspicious_card_list AS suspicious_cards;
END //
DELIMITER ;