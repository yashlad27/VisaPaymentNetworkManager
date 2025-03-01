### Basic Queries

1. **View all cardholders**
```sql
SELECT * FROM cardholders;
```

2. **View all transactions**
```sql
SELECT * FROM transactions;
```

3. **View all cards**
```sql
SELECT * FROM cards;
```

### Joins

4. **Cardholders with their cards**
```sql
SELECT c.cardholder_id, c.first_name, c.last_name, c.email, 
       cd.card_id, cd.card_type, cd.expiry_date, cd.is_active 
FROM cardholders c
JOIN cards cd ON c.cardholder_id = cd.cardholder_id;
```

5. **Merchants with transaction details**
```sql
SELECT m.merchant_id, m.merchant_name, m.merchant_category_code,
       t.transaction_id, t.amount, t.currency, t.transaction_status
FROM merchants m
JOIN transactions t ON m.merchant_id = t.merchant_id;
```

6. **Complete transaction details with all related entities**
```sql
SELECT t.transaction_id, t.amount, t.transaction_timestamp, t.transaction_status,
       ch.first_name, ch.last_name, c.card_type,
       m.merchant_name, m.merchant_category_code,
       a.auth_code, ar.response_code, ar.response_message
FROM transactions t
JOIN cards c ON t.card_id = c.card_id
JOIN cardholders ch ON c.cardholder_id = ch.cardholder_id
JOIN merchants m ON t.merchant_id = m.merchant_id
LEFT JOIN authorizations a ON t.transaction_id = a.transaction_id
LEFT JOIN authorization_responses ar ON a.authorization_id = ar.authorization_id;
```

### Aggregates

7. **Transaction count by status**
```sql
SELECT transaction_status, COUNT(*) AS total_count
FROM transactions
GROUP BY transaction_status;
```

8. **Total transaction amount by merchant**
```sql
SELECT m.merchant_name, SUM(t.amount) AS total_amount
FROM merchants m
JOIN transactions t ON m.merchant_id = t.merchant_id
GROUP BY m.merchant_name
ORDER BY total_amount DESC;
```

9. **Authorization success rate**
```sql
SELECT a.status, COUNT(*) AS count, 
       (COUNT(*) / (SELECT COUNT(*) FROM authorizations)) * 100 AS percentage
FROM authorizations a
GROUP BY a.status;
```

### Views

10. **Create a view for active cards**
```sql
CREATE VIEW active_cards AS
SELECT c.card_id, c.card_type, c.expiry_date, 
       ch.first_name, ch.last_name, ch.email,
       ib.bank_name AS issuing_bank
FROM cards c
JOIN cardholders ch ON c.cardholder_id = ch.cardholder_id
JOIN issuing_banks ib ON c.issuing_bank_id = ib.issuing_bank_id
WHERE c.is_active = 1;
```

11. **Create a view for transaction summary**
```sql
CREATE VIEW transaction_summary AS
SELECT DATE_FORMAT(transaction_timestamp, '%Y-%m') AS month,
       COUNT(*) AS transaction_count,
       SUM(amount) AS total_amount,
       AVG(amount) AS average_amount
FROM transactions
GROUP BY month
ORDER BY month;
```

### Stored Procedures

12. **Call the authorize transaction stored procedure**
```sql
CALL authorize_transaction(1, 1, 100.00, 'REF123456', @transaction_id, @status, @auth_code);
SELECT @transaction_id, @status, @auth_code;
```

13. **Call the process merchant settlement stored procedure**
```sql
CALL process_merchant_settlement(1, CURDATE(), @total, @count);
SELECT @total, @count;
```

14. **Call the transaction summary report procedure**
```sql
CALL generate_transaction_summary('2023-01-01', '2023-12-31', 'monthly');
```

### Advanced Queries

15. **Find expired cards**
```sql
SELECT c.card_id, c.card_type, c.expiry_date, 
       ch.first_name, ch.last_name, ch.email
FROM cards c
JOIN cardholders ch ON c.cardholder_id = ch.cardholder_id
WHERE c.expiry_date < CURDATE();
```

16. **High-value transactions**
```sql
SELECT t.transaction_id, t.amount, t.transaction_timestamp,
       m.merchant_name, ch.first_name, ch.last_name
FROM transactions t
JOIN cards c ON t.card_id = c.card_id
JOIN cardholders ch ON c.cardholder_id = ch.cardholder_id
JOIN merchants m ON t.merchant_id = m.merchant_id
WHERE t.amount > 1000
ORDER BY t.amount DESC;
```

17. **Transactions with interchange fee calculation**
```sql
SELECT t.transaction_id, t.amount, c.card_type, m.merchant_category_code,
       i.percentage_fee, i.fixed_fee,
       (t.amount * i.percentage_fee / 100) + i.fixed_fee AS interchange_fee
FROM transactions t
JOIN cards c ON t.card_id = c.card_id
JOIN merchants m ON t.merchant_id = m.merchant_id
JOIN interchange_fees i ON c.card_type = i.card_type AND m.merchant_category_code = i.merchant_category
WHERE t.transaction_status = 'completed';
```
