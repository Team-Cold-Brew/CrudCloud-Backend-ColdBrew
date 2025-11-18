-- ============================================================================
-- CrudCloud - MercadoPago Module Database Schema
-- ============================================================================
-- Este script crea las tablas necesarias para el módulo de MercadoPago
-- Incluye: payments, subscriptions, payment_preferences
-- ============================================================================

-- ============================================================================
-- TABLA: payments
-- Almacena los pagos realizados a través de MercadoPago
-- ============================================================================
CREATE TABLE IF NOT EXISTS payments (
    payment_id BIGSERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    plan_id INTEGER NOT NULL,
    mercadopago_payment_id VARCHAR(100) UNIQUE,
    preference_id VARCHAR(100),
    amount NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'ARS',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    status_detail VARCHAR(100),
    payment_method VARCHAR(50),
    payment_type VARCHAR(50),
    description TEXT,
    external_reference VARCHAR(100),
    payer_email VARCHAR(100),
    payer_identification VARCHAR(50),
    approved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign keys
    CONSTRAINT fk_payment_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_plan FOREIGN KEY (plan_id) REFERENCES plan(plan_id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT chk_payment_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'REFUNDED', 'IN_PROCESS', 'IN_MEDIATION', 'CHARGED_BACK')),
    CONSTRAINT chk_payment_amount CHECK (amount >= 0)
);

-- Índices para payments
CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_plan_id ON payments(plan_id);
CREATE INDEX idx_payments_mercadopago_id ON payments(mercadopago_payment_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_external_reference ON payments(external_reference);
CREATE INDEX idx_payments_created_at ON payments(created_at DESC);

-- ============================================================================
-- TABLA: subscriptions
-- Almacena las suscripciones de los usuarios a planes
-- ============================================================================
CREATE TABLE IF NOT EXISTS subscriptions (
    subscription_id BIGSERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    plan_id INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    next_billing_date TIMESTAMP,
    auto_renewal BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at TIMESTAMP,
    
    -- Foreign keys
    CONSTRAINT fk_subscription_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_subscription_plan FOREIGN KEY (plan_id) REFERENCES plan(plan_id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT chk_subscription_status CHECK (status IN ('PENDING', 'ACTIVE', 'CANCELLED', 'EXPIRED', 'SUSPENDED')),
    CONSTRAINT chk_subscription_dates CHECK (end_date IS NULL OR end_date > start_date)
);

-- Índices para subscriptions
CREATE INDEX idx_subscriptions_user_id ON subscriptions(user_id);
CREATE INDEX idx_subscriptions_plan_id ON subscriptions(plan_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);
CREATE INDEX idx_subscriptions_active_user ON subscriptions(user_id, status) WHERE status = 'ACTIVE';
CREATE INDEX idx_subscriptions_renewal ON subscriptions(next_billing_date) WHERE status = 'ACTIVE' AND auto_renewal = TRUE;

-- ============================================================================
-- TABLA: payment_preferences
-- Almacena las preferencias de pago creadas en MercadoPago (Checkout Pro)
-- ============================================================================
CREATE TABLE IF NOT EXISTS payment_preferences (
    preference_id BIGSERIAL PRIMARY KEY,
    mercadopago_preference_id VARCHAR(100) UNIQUE NOT NULL,
    user_id INTEGER NOT NULL,
    plan_id INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price NUMERIC(10, 2) NOT NULL,
    currency_id VARCHAR(3) NOT NULL DEFAULT 'ARS',
    external_reference VARCHAR(100),
    init_point VARCHAR(500),
    sandbox_init_point VARCHAR(500),
    is_expired BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign keys
    CONSTRAINT fk_preference_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_preference_plan FOREIGN KEY (plan_id) REFERENCES plan(plan_id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT chk_preference_quantity CHECK (quantity > 0),
    CONSTRAINT chk_preference_unit_price CHECK (unit_price >= 0)
);

-- Índices para payment_preferences
CREATE INDEX idx_preferences_mercadopago_id ON payment_preferences(mercadopago_preference_id);
CREATE INDEX idx_preferences_user_id ON payment_preferences(user_id);
CREATE INDEX idx_preferences_plan_id ON payment_preferences(plan_id);
CREATE INDEX idx_preferences_external_reference ON payment_preferences(external_reference);
CREATE INDEX idx_preferences_active ON payment_preferences(user_id, is_expired) WHERE is_expired = FALSE;

-- ============================================================================
-- COMENTARIOS DE TABLAS Y COLUMNAS
-- ============================================================================

-- Payments
COMMENT ON TABLE payments IS 'Registro de pagos realizados a través de MercadoPago';
COMMENT ON COLUMN payments.mercadopago_payment_id IS 'ID del pago en MercadoPago';
COMMENT ON COLUMN payments.preference_id IS 'ID de la preferencia de pago utilizada';
COMMENT ON COLUMN payments.external_reference IS 'Referencia externa para identificar el pago';
COMMENT ON COLUMN payments.status IS 'Estado del pago: PENDING, APPROVED, REJECTED, etc.';

-- Subscriptions
COMMENT ON TABLE subscriptions IS 'Suscripciones de usuarios a planes';
COMMENT ON COLUMN subscriptions.status IS 'Estado de la suscripción: PENDING, ACTIVE, CANCELLED, EXPIRED, SUSPENDED';
COMMENT ON COLUMN subscriptions.auto_renewal IS 'Si la suscripción se renueva automáticamente';
COMMENT ON COLUMN subscriptions.next_billing_date IS 'Fecha del próximo cobro automático';

-- Payment Preferences
COMMENT ON TABLE payment_preferences IS 'Preferencias de pago creadas en MercadoPago Checkout Pro';
COMMENT ON COLUMN payment_preferences.mercadopago_preference_id IS 'ID de la preferencia en MercadoPago';
COMMENT ON COLUMN payment_preferences.init_point IS 'URL para iniciar el checkout en MercadoPago';
COMMENT ON COLUMN payment_preferences.sandbox_init_point IS 'URL para iniciar el checkout en modo sandbox';

-- ============================================================================
-- DATOS INICIALES (OPCIONAL)
-- ============================================================================

-- Puedes agregar datos de prueba aquí si lo necesitas

-- ============================================================================
-- FIN DEL SCRIPT
-- ============================================================================
