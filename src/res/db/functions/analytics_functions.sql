-- Calculate Transaction Success Rate
DELIMITER //
CREATE FUNCTION calculate_success_rate(
    bank_id INT UNSIGNED,
    start_date DATE,
    end_date DATE
) RETURNS DECIMAL(5,2)
DETERMINISTIC
BEGIN
    DECLARE success_rate DECIMAL(5,2);
    
    SELECT 
        (SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) / COUNT(*)) * 100
    INTO success_rate
    FROM Transaction
    WHERE acquiring_bank_id = bank_id
    AND DATE(timestamp) BETWEEN start_date AND end_date;
    
    RETURN success_rate;
END //
DELIMITER ;

-- Calculate Daily Transaction Volume
DELIMITER //
CREATE FUNCTION get_daily_transaction_volume(
    target_date DATE
) RETURNS INT
DETERMINISTIC
BEGIN
    DECLARE volume INT;
    
    SELECT COUNT(*)
    INTO volume
    FROM Transaction
    WHERE DATE(timestamp) = target_date;
    
    RETURN volume;
END //
DELIMITER ; 