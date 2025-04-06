DELIMITER //

-- Enable event scheduler
SET GLOBAL event_scheduler = ON //

-- Create an event to deactivate expired cards
CREATE EVENT expire_cards_event
ON SCHEDULE EVERY 1 DAY
STARTS CURRENT_DATE + INTERVAL 1 DAY
DO
BEGIN
    UPDATE Card
    SET is_active = FALSE
    WHERE expiry_date < CURRENT_DATE AND is_active = TRUE;
END //

-- Create an event to generate daily transaction summary
CREATE TABLE IF NOT EXISTS DailyTransactionSummary (
    summary_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    summary_date DATE,
    total_transactions INT,
    total_amount DECIMAL(15,2),
    success_rate DECIMAL(5,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) //

CREATE EVENT generate_daily_summary
ON SCHEDULE EVERY 1 DAY
STARTS CURRENT_DATE + INTERVAL 1 DAY
DO
BEGIN
    INSERT INTO DailyTransactionSummary(summary_date, total_transactions, total_amount, success_rate)
    SELECT 
        DATE(NOW() - INTERVAL 1 DAY) AS summary_date,
        COUNT(*) AS total_transactions,
        SUM(amount) AS total_amount,
        (SUM(CASE WHEN status = 'Approved' THEN 1 ELSE 0 END) / COUNT(*)) * 100 AS success_rate
    FROM Transaction
    WHERE DATE(timestamp) = DATE(NOW() - INTERVAL 1 DAY);
END //

DELIMITER ;
