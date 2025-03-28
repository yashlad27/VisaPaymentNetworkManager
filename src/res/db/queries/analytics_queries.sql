-- Get Top Performing Banks
SELECT 
    ab.bank_name,
    COUNT(t.transaction_id) as total_transactions,
    SUM(t.amount) as total_amount,
    (SUM(CASE WHEN t.status = 'SUCCESS' THEN 1 ELSE 0 END) / COUNT(*)) * 100 as success_rate
FROM AcquiringBank ab
JOIN Transaction t ON ab.acquiring_bank_id = t.acquiring_bank_id
WHERE t.timestamp >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY ab.bank_name
ORDER BY total_amount DESC
LIMIT 10;

-- Get Transaction Volume by Card Type
SELECT 
    c.card_type,
    COUNT(t.transaction_id) as transaction_count,
    SUM(t.amount) as total_amount,
    AVG(t.amount) as average_amount
FROM Card c
JOIN Transaction t ON c.card_id = t.card_id
WHERE t.timestamp >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY c.card_type;

-- Get Merchant Performance Metrics
SELECT 
    pm.merchant_name,
    pm.merchant_category,
    COUNT(t.transaction_id) as total_transactions,
    SUM(t.amount) as total_amount,
    (SUM(CASE WHEN t.status = 'SUCCESS' THEN 1 ELSE 0 END) / COUNT(*)) * 100 as success_rate
FROM PaymentMerchant pm
JOIN Transaction t ON pm.merchant_id = t.merchant_id
WHERE t.timestamp >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY pm.merchant_name, pm.merchant_category
ORDER BY total_amount DESC; 