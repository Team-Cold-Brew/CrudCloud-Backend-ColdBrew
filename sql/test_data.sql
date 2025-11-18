-- ========================================
-- DATOS DE PRUEBA PARA MÓDULO MERCADOPAGO
-- ========================================

-- Insertar planes de ejemplo (si no existen)
INSERT INTO plan (plan_id, name, description, price, max_instances, features, created_at, updated_at) 
VALUES 
    (1, 'FREE', 'Plan gratuito con funcionalidades básicas', 0.00, 1, '{"storage": "1GB", "databases": 1}', NOW(), NOW()),
    (2, 'STANDARD', 'Plan estándar para uso profesional', 29.99, 5, '{"storage": "10GB", "databases": 5, "support": "email"}', NOW(), NOW()),
    (3, 'PREMIUM', 'Plan premium con todas las funcionalidades', 99.99, 20, '{"storage": "100GB", "databases": 20, "support": "24/7", "backup": true}', NOW(), NOW())
ON CONFLICT (plan_id) DO NOTHING;

-- Insertar usuarios de ejemplo (si no existen)
INSERT INTO users (user_id, username, email, password, user_type, personal_plan_id, status, created_at, updated_at)
VALUES 
    (1, 'testuser', 'test@example.com', 'password123', 'INDIVIDUAL', 1, 'ACTIVE', NOW(), NOW()),
    (2, 'adminuser', 'admin@example.com', 'admin123', 'ORGANIZATION_ADMIN', 2, 'ACTIVE', NOW(), NOW()),
    (3, 'premiumuser', 'premium@example.com', 'premium123', 'INDIVIDUAL', 3, 'ACTIVE', NOW(), NOW())
ON CONFLICT (user_id) DO NOTHING;

-- Insertar organizaciones de ejemplo (si no existen)
INSERT INTO organization (organization_id, name, description, plan_id, owner_id, status, created_at, updated_at)
VALUES 
    (1, 'Test Organization', 'Organización para pruebas', 2, 2, 'ACTIVE', NOW(), NOW()),
    (2, 'Premium Corp', 'Empresa con plan premium', 3, 3, 'ACTIVE', NOW(), NOW())
ON CONFLICT (organization_id) DO NOTHING;

-- Insertar proveedores de pago (si no existen)
INSERT INTO payment_providers (provider_id, name, active, created_at, updated_at)
VALUES 
    (1, 'MERCADO_PAGO', true, NOW(), NOW()),
    (2, 'STRIPE', false, NOW(), NOW()),
    (3, 'PAYPAL', false, NOW(), NOW())
ON CONFLICT (provider_id) DO NOTHING;

-- Insertar monedas (si no existen)
INSERT INTO currency (currency_id, name, currency)
VALUES 
    (1, 'Peso Argentino', 'ARS'),
    (2, 'Dólar Estadounidense', 'USD'),
    (3, 'Euro', 'EUR'),
    (4, 'Real Brasileño', 'BRL')
ON CONFLICT (currency_id) DO NOTHING;

-- Insertar algunas transacciones de ejemplo para testing
INSERT INTO transactions (
    provider_transaction_id, 
    amount, 
    currency_id, 
    status, 
    payment_method, 
    user_id, 
    organization_id, 
    provider_id, 
    created_at, 
    updated_at
)
VALUES 
    -- Transacción pendiente para testuser
    ('pref_test_123456', 29.99, 2, 'PENDING', NULL, 1, NULL, 1, NOW(), NOW()),
    
    -- Transacción aprobada para adminuser
    ('pref_test_789012', 99.99, 2, 'APPROVED', 'credit_card', 2, 1, 1, NOW() - INTERVAL '1 day', NOW()),
    
    -- Transacción rechazada para premiumuser
    ('pref_test_345678', 29.99, 1, 'REJECTED', 'debit_card', 3, NULL, 1, NOW() - INTERVAL '2 days', NOW())
ON CONFLICT DO NOTHING;

-- Verificar datos insertados
SELECT 'PLANES INSERTADOS:' as info;
SELECT plan_id, name, price FROM plan ORDER BY plan_id;

SELECT 'USUARIOS INSERTADOS:' as info;
SELECT user_id, username, email, user_type FROM users ORDER BY user_id;

SELECT 'ORGANIZACIONES INSERTADAS:' as info;
SELECT organization_id, name, plan_id, owner_id FROM organization ORDER BY organization_id;

SELECT 'PROVEEDORES DE PAGO:' as info;
SELECT provider_id, name, active FROM payment_providers ORDER BY provider_id;

SELECT 'MONEDAS:' as info;
SELECT currency_id, name, currency FROM currency ORDER BY currency_id;

SELECT 'TRANSACCIONES DE PRUEBA:' as info;
SELECT 
    transaction_id,
    provider_transaction_id,
    amount,
    c.currency as currency,
    status,
    u.username,
    o.name as organization
FROM transactions t
LEFT JOIN currency c ON t.currency_id = c.currency_id
LEFT JOIN users u ON t.user_id = u.user_id
LEFT JOIN organization o ON t.organization_id = o.organization_id
ORDER BY t.transaction_id;
