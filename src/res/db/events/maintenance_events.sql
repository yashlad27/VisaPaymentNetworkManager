-- Daily Transaction Cleanup
DELIMITER //
CREATE EVENT daily_transaction_cleanup
ON SCHEDULE EVERY 1 DAY
DO
BEGIN
    -- Archive transactions older than 90 days
    INSERT INTO ArchivedTransactions
    SELECT * FROM Transaction
    WHERE timestamp < DATE_SUB(NOW(), INTERVAL 90 DAY);
    
    -- Delete archived transactions
    DELETE FROM Transaction
    WHERE timestamp < DATE_SUB(NOW(), INTERVAL 90 DAY);
END //
DELIMITER ;

-- Weekly Bank Performance Report
DELIMITER //
CREATE EVENT weekly_bank_report
ON SCHEDULE EVERY 1 WEEK
DO
BEGIN
    -- Generate weekly bank performance metrics
    INSERT INTO BankPerformanceReport (
        bank_id,
        total_transactions,
        success_rate,
        average_amount,
        report_date
    )
    SELECT 
        t.acquiring_bank_id,
        COUNT(*) as total_transactions,
        SUM(CASE WHEN t.status = 'SUCCESS' THEN 1 ELSE 0 END) / COUNT(*) as success_rate,
        AVG(t.amount) as average_amount,
        CURDATE() as report_date
    FROM Transaction t
    WHERE t.timestamp >= DATE_SUB(NOW(), INTERVAL 7 DAY)
    GROUP BY t.acquiring_bank_id;
END //
DELIMITER ; 