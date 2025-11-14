-- ==========================================
-- PLAN (Created first due to users.personal_plan_id dependency)
-- ==========================================
CREATE TABLE plan (
    plan_id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    max_databases INT NOT NULL,
    price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    billing_cycle VARCHAR(10) DEFAULT 'monthly' CHECK (billing_cycle IN ('monthly', 'yearly')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- ==========================================
-- USERS
-- ==========================================
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NULLABLE,
    first_name VARCHAR(100) NULLABLE,
    last_name VARCHAR(100) NULLABLE,
    profile_picture_url VARCHAR(500) NULLABLE,
    google_id VARCHAR(255) UNIQUE NULLABLE,
    github_id VARCHAR(255) UNIQUE NULLABLE,
    oauth_provider VARCHAR(20) NULLABLE,
    user_type VARCHAR(20) NOT NULL DEFAULT 'INDIVIDUAL' CHECK (user_type IN ('INDIVIDUAL', 'ORGANIZATIONAL_USER')),
    personal_plan_id INT,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (personal_plan_id) REFERENCES plan(plan_id) ON DELETE SET NULL
);

-- ==========================================
-- USER_OAUTH_PROVIDERS (Tracking multiple OAuth providers per user)
-- ==========================================
CREATE TABLE user_oauth_providers (
    provider_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    provider VARCHAR(50) NOT NULL COMMENT 'GOOGLE or GITHUB',
    provider_user_id VARCHAR(255) NOT NULL UNIQUE,
    provider_email VARCHAR(255),
    provider_name VARCHAR(255),
    linked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_provider (user_id, provider),
    INDEX idx_provider_user_id (provider_user_id)
);

-- ==========================================
-- ORGANIZATION
-- ==========================================
CREATE TABLE organization (
    organization_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    plan_id INT NOT NULL,
    status VARCHAR(15) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (plan_id) REFERENCES plan(plan_id) ON DELETE RESTRICT
);

-- ==========================================
-- ORGANIZATION_MEMBERS (Join table for many-to-many with role)
-- ==========================================
CREATE TABLE organization_members (
    organization_member_id SERIAL PRIMARY KEY,
    organization_id INT NOT NULL,
    user_id INT NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER')),
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organization(organization_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE (organization_id, user_id)
);

-- ==========================================
-- DATABASE
-- ==========================================
CREATE TABLE database (
    database_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    user_id INT NOT NULL,
    organization_id INT,
    status VARCHAR(15) NOT NULL DEFAULT 'CREATING' CHECK (status IN ('CREATING', 'RUNNING', 'SUSPENDED', 'DELETED')),
    db_type VARCHAR(15) NOT NULL CHECK (db_type IN ('MYSQL', 'POSTGRESQL', 'MONGODB', 'REDIS', 'CASSANDRA', 'SQLSERVER')),
    host VARCHAR(100) NOT NULL,
    port INT NOT NULL,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    container_id VARCHAR(255),
    pdf_download_status BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (organization_id) REFERENCES organization(organization_id) ON DELETE CASCADE
);

-- NOTE: Business rule enforcement (database creator must be org member if organization_id is set)
-- is handled at the application layer in DatabaseService.
-- This constraint cannot be enforced in PostgreSQL CHECK constraints due to subquery limitations.

-- ==========================================
-- PAYMENT PROVIDERS
-- ==========================================
CREATE TABLE payment_providers (
    provider_id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- CURRENCY
-- ==========================================
CREATE TABLE currency (
    currency_id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    currency VARCHAR(15) NOT NULL UNIQUE
);

-- ==========================================
-- TRANSACTIONS
-- ==========================================
CREATE TABLE transactions (
    transaction_id SERIAL PRIMARY KEY,
    organization_id INT,
    user_id INT NOT NULL,
    provider_id INT NOT NULL,
    currency_id INT,
    provider_transaction_id VARCHAR(300) UNIQUE NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(15) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected', 'refunded')),
    payment_method VARCHAR(50),
    approval_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organization(organization_id) ON DELETE SET NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (provider_id) REFERENCES payment_providers(provider_id) ON DELETE RESTRICT,
    FOREIGN KEY (currency_id) REFERENCES currency(currency_id) ON DELETE SET NULL
);

-- ==========================================
-- INDEXES FOR PERFORMANCE & QUERY OPTIMIZATION
-- ==========================================

-- ========== USERS INDEXES ==========
-- Index for soft delete queries (find active users)
CREATE INDEX idx_users_deleted_at ON users(deleted_at);

-- Index for login queries (email lookup)
CREATE INDEX idx_users_email ON users(email);

-- Index for username lookup
CREATE INDEX idx_users_username ON users(username);

-- Composite index for user type + active status queries
CREATE INDEX idx_users_type_status ON users(user_type, status) WHERE deleted_at IS NULL;

-- Index for plan queries (users with specific personal plan)
CREATE INDEX idx_users_personal_plan_id ON users(personal_plan_id);

-- ========== OAUTH INDEXES ==========
-- Index for OAuth lookups
CREATE INDEX idx_google_id ON users(google_id);
CREATE INDEX idx_github_id ON users(github_id);
CREATE INDEX idx_oauth_provider ON users(oauth_provider);

-- Indexes for user_oauth_providers table
CREATE INDEX idx_user_oauth_providers_user_id ON user_oauth_providers(user_id);
CREATE INDEX idx_user_oauth_providers_provider ON user_oauth_providers(provider);
CREATE INDEX idx_user_oauth_providers_linked_at ON user_oauth_providers(linked_at);

-- ========== ORGANIZATION INDEXES ==========
-- Index for soft delete queries
CREATE INDEX idx_organization_deleted_at ON organization(deleted_at);

-- Index for organization name searches
CREATE INDEX idx_organization_name ON organization(name);

-- Index for finding organizations by plan
CREATE INDEX idx_organization_plan_id ON organization(plan_id);

-- Composite index for active organizations
CREATE INDEX idx_organization_status_active ON organization(status) WHERE deleted_at IS NULL;

-- ========== ORGANIZATION_MEMBERS INDEXES ==========
-- Index for finding all members of an organization
CREATE INDEX idx_org_members_organization_id ON organization_members(organization_id);

-- Index for finding all organizations a user is member of
CREATE INDEX idx_org_members_user_id ON organization_members(user_id);

-- Index for finding specific user role in organization
CREATE INDEX idx_org_members_role ON organization_members(role);

-- Composite index for checking membership and role
CREATE INDEX idx_org_members_org_user ON organization_members(organization_id, user_id, role);

-- ========== DATABASE INDEXES ==========
-- Index for soft delete queries
CREATE INDEX idx_database_deleted_at ON database(deleted_at);

-- Index for finding databases by user (personal databases or audit trail)
CREATE INDEX idx_database_user_id ON database(user_id);

-- Index for finding databases by organization
CREATE INDEX idx_database_organization_id ON database(organization_id);

-- Index for finding databases by status (running, suspended, etc.)
CREATE INDEX idx_database_status ON database(status);

-- Index for finding databases by database type
CREATE INDEX idx_database_db_type ON database(db_type);

-- Composite index for database listing by org + status
CREATE INDEX idx_database_org_status ON database(organization_id, status, deleted_at);

-- Composite index for database listing by user + status
CREATE INDEX idx_database_user_status ON database(user_id, status, deleted_at);

-- Composite index for port allocation queries (avoid duplicates)
CREATE INDEX idx_database_port_host ON database(port, host) WHERE status != 'DELETED' AND deleted_at IS NULL;

-- Composite index for container lookup
CREATE INDEX idx_database_container_id ON database(container_id) WHERE deleted_at IS NULL;

-- ========== TRANSACTIONS INDEXES ==========
-- Index for finding transactions by organization
CREATE INDEX idx_transactions_organization_id ON transactions(organization_id);

-- Index for finding transactions by user
CREATE INDEX idx_transactions_user_id ON transactions(user_id);

-- Index for finding transactions by provider
CREATE INDEX idx_transactions_provider_id ON transactions(provider_id);

-- Index for finding transactions by status
CREATE INDEX idx_transactions_status ON transactions(status);

-- Index for finding transactions by payment method
CREATE INDEX idx_transactions_payment_method ON transactions(payment_method);

-- Composite index for transaction history by org
CREATE INDEX idx_transactions_org_status ON transactions(organization_id, status, created_at DESC);

-- Composite index for transaction history by user
CREATE INDEX idx_transactions_user_status ON transactions(user_id, status, created_at DESC);

-- Index for transaction lookup by provider ID (webhook reconciliation)
CREATE INDEX idx_transactions_provider_tx_id ON transactions(provider_transaction_id);

-- Composite index for audit queries (user activity)
CREATE INDEX idx_transactions_user_created ON transactions(user_id, created_at DESC);

-- ========== ORGANIZATION_MEMBERS AUDIT INDEXES ==========
-- Index for finding membership by join date (audit trail)
CREATE INDEX idx_org_members_joined_at ON organization_members(joined_at DESC);

-- ========== PLAN INDEXES ==========
-- Index for finding plans by name (lookups)
CREATE INDEX idx_plan_name ON plan(name);

-- ========== PAYMENT_PROVIDERS INDEXES ==========
-- Index for finding active providers
CREATE INDEX idx_payment_providers_active ON payment_providers(active);

-- ========== CURRENCY INDEXES ==========
-- Index for finding currency by code
CREATE INDEX idx_currency_code ON currency(currency);
