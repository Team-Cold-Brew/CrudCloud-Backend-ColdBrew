-- ========================================
-- DATOS DE PRUEBA PARA MERCADOPAGO
-- Actualizado para el nuevo esquema
-- ========================================

-- ==========================================
-- 1. INSERTAR PLANES
-- ==========================================
INSERT INTO plan (name, description, max_databases, price, billing_cycle, created_at, updated_at) 
VALUES 
    ('Free', 'Plan gratuito con funcionalidades básicas', 1, 0.00, 'monthly', NOW(), NOW()),
    ('Standard', 'Plan estándar para uso profesional', 5, 2999.00, 'monthly', NOW(), NOW()),
    ('Premium', 'Plan premium con todas las funcionalidades', 20, 9999.00, 'monthly', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ==========================================
-- 2. INSERTAR USUARIOS DE PRUEBA
-- ==========================================
-- Nota: password es 'password123' hasheado con BCrypt
INSERT INTO users (username, email, password, first_name, last_name, user_type, personal_plan_id, status, created_at, updated_at)
VALUES 
    ('testuser', 'test@crudcloud.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYzpLHJ4tSi', 'Test', 'User', 'INDIVIDUAL', 1, 'ACTIVE', NOW(), NOW()),
    ('standarduser', 'standard@crudcloud.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYzpLHJ4tSi', 'Standard', 'User', 'INDIVIDUAL', 2, 'ACTIVE', NOW(), NOW()),
    ('premiumuser', 'premium@crudcloud.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYzpLHJ4tSi', 'Premium', 'User', 'INDIVIDUAL', 3, 'ACTIVE', NOW(), NOW()),
    ('orgadmin', 'admin@company.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYzpLHJ4tSi', 'Organization', 'Admin', 'ORGANIZATIONAL_USER', 2, 'ACTIVE', NOW(), NOW())
ON CONFLICT (username) DO NOTHING;

-- ==========================================
-- 3. INSERTAR ORGANIZACIONES
-- ==========================================
INSERT INTO organization (name, plan_id, status, created_at, updated_at)
VALUES 
    ('Test Organization', 2, 'ACTIVE', NOW(), NOW()),
    ('Premium Corp', 3, 'ACTIVE', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ==========================================
-- 4. ASIGNAR MIEMBROS A ORGANIZACIONES
-- ==========================================
INSERT INTO organization_members (organization_id, user_id, role, joined_at)
VALUES 
    (1, 4, 'OWNER', NOW()),
    (2, 3, 'OWNER', NOW())
ON CONFLICT (organization_id, user_id) DO NOTHING;

-- ==========================================
-- 5. INSERTAR PROVEEDORES DE PAGO
-- ==========================================
INSERT INTO payment_providers (name, active, created_at, updated_at)
VALUES 
    ('MERCADOPAGO', true, NOW(), NOW()),
    ('STRIPE', false, NOW(), NOW()),
    ('PAYPAL', false, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ==========================================
-- 6. INSERTAR MONEDAS
-- ==========================================
INSERT INTO currency (name, currency)
VALUES 
    ('Peso Argentino', 'ARS'),
    ('Dólar Estadounidense', 'USD'),
    ('Euro', 'EUR'),
    ('Real Brasileño', 'BRL')
ON CONFLICT (currency) DO NOTHING;

-- ==========================================
-- VERIFICACIÓN DE DATOS INSERTADOS
-- ==========================================

SELECT '============ PLANES DISPONIBLES ============' as info;
SELECT plan_id, name, price, max_databases, billing_cycle FROM plan ORDER BY plan_id;

SELECT '============ USUARIOS DE PRUEBA ============' as info;
SELECT user_id, username, email, first_name, last_name, user_type, personal_plan_id, status FROM users ORDER BY user_id;

SELECT '============ ORGANIZACIONES ============' as info;
SELECT o.organization_id, o.name, p.name as plan_name, o.status 
FROM organization o
LEFT JOIN plan p ON o.plan_id = p.plan_id
ORDER BY o.organization_id;

SELECT '============ MIEMBROS DE ORGANIZACIONES ============' as info;
SELECT om.organization_id, o.name as organization_name, u.username, om.role 
FROM organization_members om
LEFT JOIN organization o ON om.organization_id = o.organization_id
LEFT JOIN users u ON om.user_id = u.user_id
ORDER BY om.organization_id, om.role;

SELECT '============ PROVEEDORES DE PAGO ============' as info;
SELECT provider_id, name, active FROM payment_providers ORDER BY provider_id;

SELECT '============ MONEDAS DISPONIBLES ============' as info;
SELECT currency_id, name, currency FROM currency ORDER BY currency_id;

-- ==========================================
-- INSTRUCCIONES DE USO
-- ==========================================
/*
PARA PROBAR EL FLUJO DE MERCADOPAGO EN POSTMAN:

1. CREAR CHECKOUT PREFERENCE:
   POST http://localhost:8080/api/payments/checkout
   Body: {
     "userId": 1,
     "planId": 2
   }
   
   Respuesta incluirá:
   - preferenceId: ID de la preferencia en MercadoPago
   - initPoint: URL de pago en producción (no usar)
   - sandboxInitPoint: URL de pago en sandbox (usar esta)
   
2. PAGAR EN EL SANDBOX:
   - Abrir sandboxInitPoint en el navegador
   - Usar tarjetas de prueba de MercadoPago:
     * APROBADA: 4509 9535 6623 3704 (CVV: 123, Fecha: cualquiera futura)
     * RECHAZADA: 4000 0000 0000 0002
   
3. SIMULAR WEBHOOK (sin ngrok):
   POST http://localhost:8080/api/webhooks/mercadopago/test
   Body: {
     "paymentId": "<payment_id_de_mercadopago>"
   }
   
4. VERIFICAR PAGO:
   GET http://localhost:8080/api/payments/user/1
   
5. VERIFICAR SUSCRIPCIÓN:
   GET http://localhost:8080/api/subscriptions/user/1/active

USUARIOS DE PRUEBA:
- testuser (ID: 1) - Plan Free
- standarduser (ID: 2) - Plan Standard
- premiumuser (ID: 3) - Plan Premium
- orgadmin (ID: 4) - Usuario organizacional

Todos los usuarios tienen password: password123
*/
