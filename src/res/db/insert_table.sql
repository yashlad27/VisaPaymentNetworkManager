-- Use the database
USE visa_final_spring;

-- Cardholder inserts (no changes needed)
INSERT INTO Cardholder (first_name, last_name, email, phone) VALUES
('John', 'Doe', 'john.doe@example.com', '+1-202-555-0143'),
('Emma', 'Smith', 'emma.smith@example.com', '+1-415-555-0198'),
('Liam', 'Johnson', 'liam.johnson@example.com', '+1-305-555-0172'),
('Sophia', 'Williams', 'sophia.williams@example.com', '+1-617-555-0136'),
('Michael', 'Brown', 'michael.brown@example.com', '+1-718-555-0183'),
('Olivia', 'Davis', 'olivia.davis@example.com', '+1-213-555-0123'),
('Noah', 'Martinez', 'noah.martinez@example.com', '+1-646-555-0192'),
('Isabella', 'Garcia', 'isabella.garcia@example.com', '+1-510-555-0148'),
('Ethan', 'Anderson', 'ethan.anderson@example.com', '+1-702-555-0139'),
('Ava', 'Thomas', 'ava.thomas@example.com', '+1-415-555-0187'),
('James', 'Wilson', 'james.wilson@example.com', '+1-312-555-0155'),
('Charlotte', 'Taylor', 'charlotte.taylor@example.com', '+1-404-555-0162'),
('Benjamin', 'Miller', 'benjamin.miller@example.com', '+1-512-555-0178'),
('Mia', 'Jones', 'mia.jones@example.com', '+1-206-555-0191'),
('Alexander', 'Clark', 'alexander.clark@example.com', '+1-619-555-0147'),
('Harper', 'Lewis', 'harper.lewis@example.com', '+1-773-555-0159'),
('Daniel', 'Lee', 'daniel.lee@example.com', '+1-214-555-0182'),
('Amelia', 'Walker', 'amelia.walker@example.com', '+1-303-555-0196'),
('Matthew', 'Hall', 'matthew.hall@example.com', '+1-602-555-0137'),
('Evelyn', 'Young', 'evelyn.young@example.com', '+1-860-555-0164'),
('Jackson', 'Allen', 'jackson.allen@example.com', '+1-615-555-0129'),
('Abigail', 'King', 'abigail.king@example.com', '+1-408-555-0175'),
('Aiden', 'Wright', 'aiden.wright@example.com', '+1-713-555-0188'),
('Elizabeth', 'Scott', 'elizabeth.scott@example.com', '+1-480-555-0143'),
('Lucas', 'Green', 'lucas.green@example.com', '+1-919-555-0156');

-- IssuingBank inserts (added created_at with DEFAULT)
INSERT INTO IssuingBank (bank_name, bank_code, is_active) VALUES
('Bank of America', 'BOA123', 1),
('Wells Fargo', 'WF456', 1),
('JPMorgan Chase', 'JPMC789', 1),
('Citibank', 'CITI321', 1),
('Capital One', 'CO654', 1),
('US Bank', 'USB987', 1),
('PNC Bank', 'PNC654', 1),
('TD Bank', 'TDB321', 1),
('HSBC', 'HSBC789', 1),
('American Express Bank', 'AMEX456', 1),
('Barclays', 'BARC123', 1),
('SunTrust Bank', 'SUN987', 1),
('Discover Bank', 'DISC654', 1);

-- Card inserts (no changes needed)
INSERT INTO Card (card_num_hash, card_type, expiry_date, is_active, cardholder_id, issuing_bank_id) VALUES
(MD5('4111111111111111'), 'VISA', '2027-06-30', 1, 1, 1),
(MD5('5500000000000004'), 'Mastercard', '2025-12-31', 1, 2, 2),
(MD5('340000000000009'), 'AMEX', '2026-09-30', 1, 3, 3),
(MD5('6011000000000004'), 'Discover', '2028-05-31', 1, 4, 4),
(MD5('3530111333300000'), 'JCB', '2026-11-30', 1, 5, 5),
(MD5('4222222222222222'), 'VISA', '2026-08-31', 1, 6, 1),
(MD5('5555555555554444'), 'Mastercard', '2028-01-31', 1, 7, 2),
(MD5('371449635398431'), 'AMEX', '2027-11-30', 1, 8, 3),
(MD5('6011111111111117'), 'Discover', '2026-03-31', 1, 9, 4),
(MD5('3566002020360505'), 'JCB', '2029-10-31', 1, 10, 5),
(MD5('4916338506082832'), 'VISA', '2027-04-30', 1, 11, 6),
(MD5('5281037048916168'), 'Mastercard', '2028-07-31', 1, 12, 7),
(MD5('374245455400126'), 'AMEX', '2026-01-31', 1, 13, 8),
(MD5('6011000990139424'), 'Discover', '2029-02-28', 1, 14, 4),
(MD5('3588276506632487'), 'JCB', '2027-05-31', 1, 15, 5),
(MD5('4024007198530322'), 'VISA', '2027-12-31', 1, 16, 1),
(MD5('5425233430109903'), 'Mastercard', '2029-04-30', 1, 17, 2),
(MD5('378282246310005'), 'AMEX', '2028-03-31', 1, 18, 3),
(MD5('6011622222222229'), 'Discover', '2027-07-31', 1, 19, 4),
(MD5('3530111333300001'), 'JCB', '2026-06-30', 1, 20, 5);

-- PaymentMerchant inserts (no changes needed)
INSERT INTO PaymentMerchant (merchant_name, merchant_category, terminal_id, processing_fee) VALUES
('Amazon', 'E-Commerce', 'TERM123', 2.5),
('Walmart', 'Retail', 'TERM456', 2.2),
('Apple Store', 'Electronics', 'TERM789', 3.0),
('Netflix', 'Subscription Services', 'TERM012', 1.8),
('Starbucks', 'Food & Beverage', 'TERM345', 2.1),
('Target', 'Retail', 'TERM567', 2.3),
('Best Buy', 'Electronics', 'TERM678', 2.7),
('Uber', 'Transportation', 'TERM789A', 2.0),
('Airbnb', 'Hospitality', 'TERM890', 3.2),
('DoorDash', 'Food Delivery', 'TERM901', 2.6),
('Expedia', 'Travel', 'TERM012A', 2.9),
('Spotify', 'Subscription Services', 'TERM123A', 1.9),
('Home Depot', 'Home Improvement', 'TERM234', 2.4),
('Sephora', 'Beauty', 'TERM345A', 2.5),
('Whole Foods', 'Grocery', 'TERM456A', 2.1);

-- AcquiringBank inserts (no changes needed)
INSERT INTO AcquiringBank (bank_name, bank_code, settlement_account, is_active) VALUES
('Stripe', 'STRP123', 'SETTLE001', 1),
('PayPal', 'PYPL456', 'SETTLE002', 1),
('Square', 'SQR789', 'SETTLE003', 1),
('Adyen', 'ADYN321', 'SETTLE004', 1),
('WorldPay', 'WORL654', 'SETTLE005', 1),
('Fiserv', 'FISV123', 'SETTLE006', 1),
('Global Payments', 'GLOB456', 'SETTLE007', 1),
('FIS', 'FIS789', 'SETTLE008', 1),
('TSYS', 'TSYS321', 'SETTLE009', 1),
('Elavon', 'ELAV654', 'SETTLE010', 1),
('Authorize.Net', 'AUTH987', 'SETTLE011', 1),
('Braintree', 'BRAI654', 'SETTLE012', 1),
('Chase Paymentech', 'CHAS321', 'SETTLE013', 1);

-- Exchange inserts (no changes needed)
INSERT INTO Exchange (exchange_date, status) VALUES
('2025-03-15', 'Completed'),
('2025-03-16', 'Completed'),
('2025-03-17', 'Pending'),
('2025-03-18', 'Completed'),
('2025-03-19', 'Completed'),
('2025-03-20', 'Pending'),
('2025-03-21', 'Completed'),
('2025-03-22', 'Pending'),
('2025-03-23', 'Completed'),
('2025-03-24', 'Completed');

-- Transaction inserts (no changes needed)
INSERT INTO Transaction (amount, currency, timestamp, status, card_id, merchant_id, acquiring_bank_id, exchange_id) VALUES
(129.99, 'USD', '2025-03-15 14:30:00', 'Approved', 1, 1, 1, 1),
(250.00, 'USD', '2025-03-16 09:45:00', 'Declined', 2, 2, 2, 2),
(12.99, 'USD', '2025-03-17 18:15:00', 'Approved', 3, 5, 3, 3),
(599.99, 'USD', '2025-03-18 20:50:00', 'Pending', 4, 3, 4, 4),
(9.99, 'USD', '2025-03-19 07:10:00', 'Approved', 5, 4, 5, 5),
(45.75, 'USD', '2025-03-19 10:15:00', 'Approved', 6, 6, 6, 5),
(199.99, 'USD', '2025-03-19 12:30:00', 'Approved', 7, 7, 7, 5),
(76.50, 'USD', '2025-03-19 15:45:00', 'Declined', 8, 8, 8, NULL),
(124.85, 'USD', '2025-03-20 09:20:00', 'Approved', 9, 9, 9, 6),
(39.99, 'USD', '2025-03-20 11:10:00', 'Approved', 10, 10, 10, 6),
(299.50, 'USD', '2025-03-20 14:25:00', 'Pending', 11, 11, 1, NULL),
(14.99, 'USD', '2025-03-21 08:30:00', 'Approved', 12, 12, 2, 7),
(85.75, 'USD', '2025-03-21 13:15:00', 'Approved', 13, 13, 3, 7),
(149.99, 'USD', '2025-03-21 16:45:00', 'Declined', 14, 14, 4, NULL),
(62.50, 'USD', '2025-03-22 10:05:00', 'Approved', 15, 15, 5, 8),
(899.99, 'USD', '2025-03-22 11:55:00', 'Approved', 16, 1, 6, 8),
(27.45, 'USD', '2025-03-22 14:20:00', 'Pending', 17, 2, 7, NULL),
(49.99, 'USD', '2025-03-23 09:10:00', 'Approved', 18, 3, 8, 9),
(152.30, 'USD', '2025-03-23 12:40:00', 'Approved', 19, 4, 9, 9),
(79.95, 'USD', '2025-03-23 15:25:00', 'Declined', 20, 5, 10, NULL),
(34.99, 'USD', '2025-03-24 08:55:00', 'Approved', 1, 6, 1, 10),
(215.80, 'USD', '2025-03-24 11:30:00', 'Approved', 2, 7, 2, 10),
(7.99, 'USD', '2025-03-24 14:05:00', 'Pending', 3, 8, 3, NULL),
(59.99, 'USD', '2025-03-25 09:45:00', 'Approved', 4, 9, 4, NULL),
(189.50, 'USD', '2025-03-25 12:15:00', 'Approved', 5, 10, 5, NULL);

-- Authorization inserts (Changed to match auth_id as primary key)
INSERT INTO Authorization (auth_code, timestamp, status, transaction_id) VALUES
('AUTH001', '2025-03-15 14:31:00', 'Approved', 1),
('AUTH002', '2025-03-16 09:46:00', 'Declined', 2),
('AUTH003', '2025-03-17 18:16:00', 'Approved', 3),
('AUTH004', '2025-03-18 20:51:00', 'Pending', 4),
('AUTH005', '2025-03-19 07:11:00', 'Approved', 5),
('AUTH006', '2025-03-19 10:16:00', 'Approved', 6),
('AUTH007', '2025-03-19 12:31:00', 'Approved', 7),
('AUTH008', '2025-03-19 15:46:00', 'Declined', 8),
('AUTH009', '2025-03-20 09:21:00', 'Approved', 9),
('AUTH010', '2025-03-20 11:11:00', 'Approved', 10),
('AUTH011', '2025-03-20 14:26:00', 'Pending', 11),
('AUTH012', '2025-03-21 08:31:00', 'Approved', 12),
('AUTH013', '2025-03-21 13:16:00', 'Approved', 13),
('AUTH014', '2025-03-21 16:46:00', 'Declined', 14),
('AUTH015', '2025-03-22 10:06:00', 'Approved', 15),
('AUTH016', '2025-03-22 11:56:00', 'Approved', 16),
('AUTH017', '2025-03-22 14:21:00', 'Pending', 17),
('AUTH018', '2025-03-23 09:11:00', 'Approved', 18),
('AUTH019', '2025-03-23 12:41:00', 'Approved', 19),
('AUTH020', '2025-03-23 15:26:00', 'Declined', 20),
('AUTH021', '2025-03-24 08:56:00', 'Approved', 21),
('AUTH022', '2025-03-24 11:31:00', 'Approved', 22),
('AUTH023', '2025-03-24 14:06:00', 'Pending', 23),
('AUTH024', '2025-03-25 09:46:00', 'Approved', 24),
('AUTH025', '2025-03-25 12:16:00', 'Approved', 25);

-- AuthResponse inserts (Added as required by schema)
INSERT INTO AuthResponse (response_code, response_message, auth_id) VALUES
('00', 'Approved', 1),
('05', 'Declined - Do not honor', 2),
('00', 'Approved', 3),
('85', 'Pending - Verification Required', 4),
('00', 'Approved', 5),
('00', 'Approved', 6),
('00', 'Approved', 7),
('51', 'Declined - Insufficient funds', 8),
('00', 'Approved', 9),
('00', 'Approved', 10),
('85', 'Pending - Verification Required', 11),
('00', 'Approved', 12),
('00', 'Approved', 13),
('61', 'Declined - Exceeds withdrawal limit', 14),
('00', 'Approved', 15),
('00', 'Approved', 16),
('85', 'Pending - Verification Required', 17),
('00', 'Approved', 18),
('00', 'Approved', 19),
('05', 'Declined - Do not honor', 20),
('00', 'Approved', 21),
('00', 'Approved', 22),
('85', 'Pending - Verification Required', 23),
('00', 'Approved', 24),
('00', 'Approved', 25);

-- Settlement inserts (no changes needed)
INSERT INTO Settlement (transaction_id) VALUES
(1),
(3),
(5),
(6),
(7),
(9),
(10),
(12),
(13),
(15),
(16),
(18),
(19),
(21),
(22),
(24),
(25);

-- InterchangeFee inserts (Using fee_id as primary key)
INSERT INTO InterchangeFee (exchange_id, card_type, percentage_fee, fixed_fee) VALUES
(1, 'VISA', 1.50, 0.10),
(2, 'Mastercard', 1.75, 0.15),
(3, 'AMEX', 2.00, 0.20),
(4, 'VISA', 1.55, 0.12),
(5, 'Mastercard', 1.80, 0.17),
(6, 'AMEX', 2.10, 0.22),
(7, 'Discover', 1.70, 0.15),
(8, 'JCB', 1.90, 0.18),
(9, 'VISA', 1.60, 0.13),
(10, 'Mastercard', 1.85, 0.19);

-- Add sample data for different card types across exchanges
INSERT INTO InterchangeFee (exchange_id, card_type, percentage_fee, fixed_fee) VALUES
(1, 'Mastercard', 1.65, 0.12),
(1, 'AMEX', 2.20, 0.25),
(1, 'Discover', 1.60, 0.11),
(1, 'JCB', 1.85, 0.15),
(2, 'VISA', 1.52, 0.09),
(2, 'AMEX', 2.10, 0.22),
(2, 'Discover', 1.62, 0.12),
(2, 'JCB', 1.88, 0.16);

