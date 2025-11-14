-- OAuth Fields Migration for CrudCloud Users Table
-- Adds OAuth provider support and profile information fields

-- Step 1: Make password field nullable for OAuth users
ALTER TABLE users 
MODIFY COLUMN password VARCHAR(255) NULLABLE;

-- Step 2: Add OAuth-related columns to users table
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS first_name VARCHAR(100) NULLABLE AFTER password,
ADD COLUMN IF NOT EXISTS last_name VARCHAR(100) NULLABLE AFTER first_name,
ADD COLUMN IF NOT EXISTS profile_picture_url VARCHAR(500) NULLABLE AFTER last_name,
ADD COLUMN IF NOT EXISTS google_id VARCHAR(255) UNIQUE NULLABLE AFTER profile_picture_url,
ADD COLUMN IF NOT EXISTS github_id VARCHAR(255) UNIQUE NULLABLE AFTER google_id,
ADD COLUMN IF NOT EXISTS oauth_provider VARCHAR(20) NULLABLE AFTER github_id;

-- Step 3: Create user_oauth_providers table for tracking multiple OAuth providers per user
CREATE TABLE IF NOT EXISTS user_oauth_providers (
  provider_id INT PRIMARY KEY AUTO_INCREMENT,
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

-- Step 4: Add indexes for OAuth lookups
CREATE INDEX IF NOT EXISTS idx_google_id ON users(google_id);
CREATE INDEX IF NOT EXISTS idx_github_id ON users(github_id);
CREATE INDEX IF NOT EXISTS idx_oauth_provider ON users(oauth_provider);
