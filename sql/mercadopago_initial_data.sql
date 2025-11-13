-- ==========================================
-- INITIAL DATA FOR MERCADOPAGO MODULE
-- ==========================================

-- Insert Payment Providers
INSERT INTO payment_providers (name, active, created_at, updated_at) VALUES
('MERCADO_PAGO', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- Insert Currencies
INSERT INTO currency (name, currency) VALUES
('Argentine Peso', 'ARS'),
('US Dollar', 'USD'),
('Euro', 'EUR'),
('Brazilian Real', 'BRL')
ON CONFLICT (currency) DO NOTHING;

-- Insert Plans (if not already exist)
INSERT INTO plan (name, description, max_instances, price, billing_cycle, created_at, updated_at) VALUES
('FREE', 'Free plan with basic features', 2, 0.00, 'monthly', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('STANDARD', 'Standard plan with enhanced features', 5, 19.99, 'monthly', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PREMIUM', 'Premium plan with all features', 10, 49.99, 'monthly', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- Update existing plans with proper pricing (if needed)
UPDATE plan SET 
    price = 0.00,
    description = 'Free plan with basic features - Auto-generated DB names'
WHERE name = 'FREE';

UPDATE plan SET 
    price = 19.99,
    description = 'Standard plan - Custom DB names, Email support'
WHERE name = 'STANDARD';

UPDATE plan SET 
    price = 49.99,
    description = 'Premium plan - All Standard features + Priority support'
WHERE name = 'PREMIUM';

-- Verify data insertion
SELECT 'Payment Providers:' as info;
SELECT provider_id, name, active FROM payment_providers;

SELECT 'Currencies:' as info;
SELECT currency_id, name, currency FROM currency;

SELECT 'Plans:' as info;
SELECT plan_id, name, max_instances, price, billing_cycle FROM plan ORDER BY price;
