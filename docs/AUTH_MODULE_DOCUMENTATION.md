# 🔐 Auth Module Documentation

**Module:** Authentication & Authorization Layer  
**Location:** `src/main/java/com/riwi/CrudCloud/auth/`  
**Purpose:** Handle user authentication (traditional & OAuth 2.0), JWT token management, and plan/subscription management  
**Version:** 1.0  
**Last Updated:** November 18, 2025

---

## 📋 Table of Contents

1. [Module Overview](#module-overview)
2. [Architecture](#architecture)
3. [Component Breakdown](#component-breakdown)
4. [Authentication Flows](#authentication-flows)
5. [API Endpoints](#api-endpoints)
6. [Security Configuration](#security-configuration)
7. [Data Models](#data-models)
8. [Exception Handling](#exception-handling)
9. [Integration Points](#integration-points)
10. [Configuration](#configuration)

---

## 🎯 Module Overview

The Auth Module is responsible for:

- ✅ **User Registration & Login:** Traditional email/password authentication with BCrypt hashing
- ✅ **OAuth 2.0 Integration:** Google and GitHub OAuth provider support
- ✅ **JWT Token Management:** Generate, validate, and manage JWT tokens for stateless authentication
- ✅ **Plan Management:** Retrieve subscription plan information and apply plan-based limits
- ✅ **Account Linking:** Link multiple OAuth providers to existing accounts
- ✅ **Security:** Implement Spring Security with JWT filters, CORS configuration, and BCrypt password encoding

---

## 🏗️ Architecture

### High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         Frontend (React)                         │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTP Requests
                             ▼
┌──────────────────────────────────────────────────────────────────┐
│                      API Gateway Layer                            │
│                   (Spring Security Filter)                        │
│              ┌─────────────────────────────────┐                 │
│              │  JwtAuthenticationFilter        │                 │
│              │  (Validate JWT Tokens)          │                 │
│              └─────────────────────────────────┘                 │
└──────────────────────────────────────────────────────────────────┘
                             │
        ┌────────────────────┼────────────────────┐
        ▼                    ▼                    ▼
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│ AuthController   │ │ OAuthController  │ │ PlanController   │
│ - Register       │ │ - OAuth Login    │ │ - Get Plans      │
│ - Login          │ │ - OAuth Callback │ │ - Get Plan Info  │
│ - Get Profile    │ │ - Link Provider  │ │                  │
└────────┬─────────┘ └────────┬─────────┘ └────────┬─────────┘
         │                    │                    │
         └────────────────────┼────────────────────┘
                             │
        ┌────────────────────┼────────────────────┐
        ▼                    ▼                    ▼
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│  AuthService     │ │OAuth*Service     │ │ PlanService      │
│                  │ │                  │ │                  │
│ - Register User  │ │ - Google OAuth   │ │ - Get All Plans  │
│ - Login User     │ │ - GitHub OAuth   │ │ - Get Plan by ID │
│ - Get Profile    │ │ - Link Account   │ │ - Get Plan ByName│
└────────┬─────────┘ └────────┬─────────┘ └────────┬─────────┘
         │                    │                    │
         └────────────────────┼────────────────────┘
                             │
        ┌────────────────────┼────────────────────┐
        ▼                    ▼                    ▼
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│ UserRepository   │ │OAuthProvider*    │ │ PlanRepository   │
│                  │ │Repository        │ │                  │
│ - Find by Email  │ │                  │ │ - Find All       │
│ - Find by ID     │ │ - Manage OAuth   │ │ - Find by ID     │
│ - Exists Check   │ │   Providers      │ │ - Find by Name   │
└────────┬─────────┘ └────────┬─────────┘ └────────┬─────────┘
         │                    │                    │
         └────────────────────┼────────────────────┘
                             │
                    ┌────────┴────────┐
                    ▼                 ▼
              ┌────────────────────────────────┐
              │      Database (PostgreSQL)     │
              │  - users table                 │
              │  - plan table                  │
              │  - user_oauth_providers table  │
              └────────────────────────────────┘
```

### Component Interaction Flow

```
User Request
    │
    ▼
SecurityFilterChain (CORS, CSRF)
    │
    ▼
JwtAuthenticationFilter (If protected endpoint)
    │ Validates Token
    ├─ If Valid → Continue
    └─ If Invalid → 401 Unauthorized
    │
    ▼
Controller Layer
    ├─ AuthController (Register, Login, Profile)
    ├─ OAuthController (OAuth Callbacks)
    └─ PlanController (Plan Data)
    │
    ▼
Service Layer
    ├─ AuthService (Auth Business Logic)
    ├─ OAuthUserProcessorService (OAuth User Processing)
    ├─ GoogleOAuthService / GitHubOAuthService (Provider Integration)
    └─ PlanService (Plan Logic)
    │
    ▼
Repository Layer
    └─ Database Access
    │
    ▼
Response to Client with JWT Token (if auth) or Data
```

---

## 🧩 Component Breakdown

### 1. **Controllers** (`controller/`)

#### **AuthController** 
**Path:** `/api/v1/auth`

Handles traditional authentication (email/password).

| Method | Endpoint | Auth Required | Description |
|--------|----------|---------------|-------------|
| POST | `/register` | ❌ | Register new user with email/password |
| POST | `/login` | ❌ | Login with email/password |
| GET | `/profile` | ⚠️ | Get user profile (requires userId param) |

**Example Request/Response:**
```bash
# Register
POST /api/v1/auth/register
{
  "email": "user@example.com",
  "username": "johndoe",
  "password": "SecurePass123!",
  "userType": "INDIVIDUAL"
}

# Response: 201 Created
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "user": {
    "userId": 1,
    "email": "user@example.com",
    "username": "johndoe",
    "userType": "INDIVIDUAL"
  },
  "message": "User registered successfully"
}
```

---

#### **OAuthController**
**Path:** `/api/v1/auth/oauth`

Handles OAuth 2.0 flows for Google and GitHub.

| Method | Endpoint | Auth Required | Description |
|--------|----------|---------------|-------------|
| POST | `/login/{provider}` | ❌ | OAuth login with code exchange |
| POST | `/callback/{provider}` | ❌ | OAuth callback handler |
| POST | `/link/{provider}` | ⚠️ | Link OAuth provider to account |

**OAuth Flow:**
```
Frontend
  │
  ├─ User clicks "Login with Google/GitHub"
  │
  ├─ Redirect to OAuth provider
  │
  ├─ User authorizes
  │
  ├─ OAuth provider redirects back with code
  │
  └─ Frontend sends code to Backend
       │
       ▼
Backend OAuthController
  │
  ├─ Validate code
  │
  ├─ Exchange code for access token
  │
  ├─ Fetch user profile from provider
  │
  ├─ Check if user exists
  │    ├─ If exists → Generate JWT
  │    └─ If new → Create user → Generate JWT
  │
  └─ Return AuthResponse with JWT token
```

---

#### **PlanController**
**Path:** `/api/v1/plans`

Provides subscription plan information.

| Method | Endpoint | Auth Required | Description |
|--------|----------|---------------|-------------|
| GET | `/` | ❌ | Get all plans |
| GET | `/{planId}` | ❌ | Get plan by ID |
| GET | `/name/{name}` | ❌ | Get plan by name |

**Example Response:**
```json
{
  "planId": 1,
  "name": "FREE",
  "description": "Free tier with basic features",
  "maxDatabases": 2,
  "price": 0.00,
  "billingCycle": "monthly"
}
```

---

### 2. **Services** (`service/`)

#### **AuthService**

Implements core authentication business logic.

**Key Methods:**

```java
// Register new user with email/password
public AuthResponse register(RegisterRequest request)
  ├─ Validate email/username uniqueness
  ├─ Hash password with BCrypt
  ├─ Create user in database
  ├─ Generate JWT token
  └─ Return AuthResponse

// Login user
public AuthResponse login(LoginRequest request)
  ├─ Find user by email
  ├─ Verify password with BCrypt
  ├─ Generate JWT token
  └─ Return AuthResponse

// Get user profile
public UserResponse getUserProfile(Integer userId)
  ├─ Find user by ID
  └─ Map to UserResponse DTO
```

**Key Features:**
- ✅ Password hashing with BCrypt (strength: 12 rounds)
- ✅ Transactional operations for data consistency
- ✅ Custom exceptions for different scenarios
- ✅ Comprehensive logging

---

#### **OAuthUserProcessorService**

Handles OAuth user creation and account linking logic.

**Key Methods:**

```java
// Process OAuth user after provider authentication
public AuthResponse processOAuthUser(OAuthUserResponse oAuthUser, OAuthProvider provider)
  ├─ Check if user exists with provider ID (returning user)
  ├─ Check if email exists (possible account linking scenario)
  │  └─ If conflict → Throw AccountLinkingException
  ├─ Create new user from OAuth data
  ├─ Generate JWT token
  └─ Return AuthResponse

// Link OAuth provider to existing user
public AuthResponse linkOAuthProvider(Integer userId, OAuthUserResponse oAuthUser, OAuthProvider provider)
  ├─ Validate user exists
  ├─ Check provider not already linked
  ├─ Link provider to user
  └─ Return AuthResponse
```

**Three Scenarios Handled:**

| Scenario | Action |
|----------|--------|
| **New OAuth User** | Create new user account with OAuth provider data |
| **Existing OAuth User** | Return existing account with JWT token |
| **Email Conflict** | Throw AccountLinkingException (email already in use by different provider) |

---

#### **GoogleOAuthService & GitHubOAuthService**

Provider-specific OAuth implementations.

**Key Methods:**

```java
// Exchange authorization code for access token
public OAuth2TokenResponse exchangeCodeForToken(String code)
  ├─ Make request to OAuth provider's token endpoint
  ├─ Include client_id, client_secret, code
  └─ Return access token

// Fetch user profile from provider
public OAuthUserResponse getUserProfile(String accessToken)
  ├─ Make request to provider's user info endpoint
  ├─ Parse response (name, email, avatar, etc.)
  └─ Return OAuthUserResponse
```

**Configuration:**
```properties
# application.properties
oauth.google.client-id=your-client-id
oauth.google.client-secret=your-client-secret
oauth.google.token-url=https://oauth2.googleapis.com/token
oauth.google.user-info-url=https://www.googleapis.com/oauth2/v2/userinfo

oauth.github.client-id=your-client-id
oauth.github.client-secret=your-client-secret
oauth.github.token-url=https://github.com/login/oauth/access_token
oauth.github.user-info-url=https://api.github.com/user
```

---

#### **PlanService**

Manages subscription plan data and retrieval.

**Key Methods:**

```java
// Get all available plans
public List<PlanResponse> getAllPlans()
  └─ Return all plans with mapping to DTO

// Get plan by ID
public PlanResponse getPlanById(Integer planId)
  ├─ Find plan or throw ResourceNotFoundException
  └─ Return PlanResponse

// Get plan by name
public PlanResponse getPlanByName(String name)
  ├─ Find plan or throw ResourceNotFoundException
  └─ Return PlanResponse
```

**Plan Tiers:**

| Plan | Max Databases | Price | Features |
|------|---------------|-------|----------|
| FREE | 2 | $0/month | Auto-generated DB names |
| STANDARD | 5 | $19.99/month | Custom names, email support |
| PREMIUM | 10 | $49.99/month | All + priority support |

---

#### **TokenService**

Generates, validates, and manages JWT tokens.

**Key Methods:**

```java
// Generate JWT token for user
public String generateToken(User user)
  ├─ Include user ID as subject
  ├─ Add claims (email, username, userType)
  ├─ Set expiration (24 hours by default)
  └─ Sign with HMAC-SHA256

// Validate JWT token
public boolean validateToken(String token)
  ├─ Parse token signature
  ├─ Check expiration
  └─ Return validity

// Extract user ID from token
public Integer extractUserId(String token)
  ├─ Parse token
  ├─ Get subject (user ID)
  └─ Return user ID

// Extract all claims
public Claims extractClaims(String token)
  └─ Return all JWT claims (email, username, etc.)
```

**JWT Token Structure:**
```
Header: {
  "alg": "HS256",
  "typ": "JWT"
}

Payload: {
  "sub": "1",                    // User ID
  "email": "user@example.com",
  "username": "johndoe",
  "userType": "INDIVIDUAL",
  "iat": 1700000000,             // Issued at
  "exp": 1700086400              // Expiration (24 hours)
}

Signature: HMAC-SHA256(header.payload, secret)
```

---

### 3. **Repositories** (`repository/`)

Data access layer using Spring Data JPA.

#### **UserRepository**

```java
// Find user by email (excluding soft-deleted users)
Optional<User> findByEmailAndNotDeleted(String email)

// Find user by email
Optional<User> findByEmail(String email)

// Check if email exists (excluding soft-deleted)
boolean existsByEmailAndNotDeleted(String email)

// Check if username exists (excluding soft-deleted)
boolean existsByUsernameAndNotDeleted(String username)

// Find by username
Optional<User> findByUsername(String username)
```

#### **PlanRepository**

```java
// Find plan by ID
Optional<Plan> findById(Integer planId)

// Find plan by name
Optional<Plan> findByName(String name)

// Get all plans
List<Plan> findAll()
```

#### **UserOAuthProviderRepository**

```java
// Find OAuth provider for user
Optional<UserOAuthProvider> findByUserAndProvider(User user, OAuthProvider provider)

// Find by provider-specific user ID
Optional<UserOAuthProvider> findByProviderUserIdAndProvider(String providerUserId, OAuthProvider provider)
```

---

### 4. **Configuration** (`config/`)

#### **SecurityConfig**

Spring Security configuration with JWT support.

**Security Features:**
- ✅ CSRF disabled (stateless API)
- ✅ CORS enabled with proper configuration
- ✅ Stateless session management (JWT)
- ✅ Password encoding with BCrypt (12 rounds)
- ✅ JWT filter chain integration
- ✅ Protected/public endpoint configuration

**Protected Endpoints:**
- All `/api/v1/**` routes except:
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/login`
  - `GET /api/v1/plans/**`
  - `POST /api/v1/auth/oauth/callback/**`
  - `POST /api/v1/payments/webhook`

---

#### **JwtAuthenticationFilter**

Custom Spring Security filter for JWT validation.

**Process:**
```
Request → JwtAuthenticationFilter
  │
  ├─ Extract JWT token from Authorization header
  │
  ├─ Validate token format (Bearer scheme)
  │
  ├─ Validate token signature and expiration
  │
  ├─ Extract user ID and claims
  │
  ├─ Load user from database
  │
  ├─ Create SecurityContext with authentication
  │
  └─ Continue to controller
```

---

#### **OAuthProviderConfig**

Configuration for OAuth providers (Google, GitHub).

Contains:
- Client IDs and secrets
- Token endpoints
- User info endpoints
- Redirect URIs

---

### 5. **DTOs** (`dto/`)

Data Transfer Objects for request/response validation and serialization.

#### **Request DTOs:**

**RegisterRequest:**
```java
{
  "email": "user@example.com",
  "username": "johndoe",
  "password": "SecurePass123!",  // Min 8 chars, special chars, etc.
  "userType": "INDIVIDUAL"        // or ORGANIZATIONAL_USER
}
```

**LoginRequest:**
```java
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**OAuthCallbackRequest:**
```java
{
  "code": "authorization-code-from-provider",
  "state": "state-parameter-for-csrf-protection",  // Optional
  "redirectUri": "http://localhost:3000/auth/callback"  // Optional
}
```

#### **Response DTOs:**

**AuthResponse:**
```java
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "user": {
    "userId": 1,
    "email": "user@example.com",
    "username": "johndoe",
    "userType": "INDIVIDUAL",
    "status": "ACTIVE"
  },
  "message": "Authentication successful"
}
```

**UserResponse:**
```java
{
  "userId": 1,
  "email": "user@example.com",
  "username": "johndoe",
  "firstName": "John",
  "lastName": "Doe",
  "userType": "INDIVIDUAL",
  "status": "ACTIVE",
  "profilePictureUrl": "https://example.com/pic.jpg",
  "oauthProvider": "GOOGLE",
  "createdAt": "2025-11-18T10:30:00Z"
}
```

**PlanResponse:**
```java
{
  "planId": 1,
  "name": "FREE",
  "description": "Free tier with basic features",
  "maxDatabases": 2,
  "price": 0.00,
  "billingCycle": "monthly"
}
```

---

## 🔐 Authentication Flows

### Flow 1: Traditional Registration

```
┌─────────────────────────────────────────────────────────────────┐
│ Traditional User Registration Flow                               │
└─────────────────────────────────────────────────────────────────┘

Frontend User
  │
  └─ Submits: email, username, password, userType
       │
       ▼
┌──────────────────────────────────────┐
│ POST /api/v1/auth/register           │
│ Payload: RegisterRequest DTO         │
└──────────────────────────────────────┘
       │
       ▼
AuthController.register()
       │
       ├─ Validate input (JSR-380 Bean Validation)
       │
       └─ Call AuthService.register()
            │
            ├─ Check email uniqueness
            │  └─ If exists → ConflictException (409)
            │
            ├─ Check username uniqueness
            │  └─ If exists → ConflictException (409)
            │
            ├─ Hash password with BCrypt(12 rounds)
            │
            ├─ Create User entity
            │  └─ Set status = ACTIVE
            │  └─ Set createdAt = now
            │
            ├─ Save to database
            │
            ├─ Generate JWT token
            │  └─ TokenService.generateToken(user)
            │
            └─ Return AuthResponse (201 Created)
                 │
                 └─ Frontend stores JWT in localStorage
                    Uses for subsequent API calls
```

---

### Flow 2: Traditional Login

```
┌─────────────────────────────────────────────────────────────────┐
│ Traditional User Login Flow                                      │
└─────────────────────────────────────────────────────────────────┘

Frontend User
  │
  └─ Submits: email, password
       │
       ▼
┌──────────────────────────────────────┐
│ POST /api/v1/auth/login              │
│ Payload: LoginRequest DTO            │
└──────────────────────────────────────┘
       │
       ▼
AuthController.login()
       │
       ├─ Validate input
       │
       └─ Call AuthService.login()
            │
            ├─ Find user by email
            │  └─ If not found → ResourceNotFoundException (404)
            │
            ├─ Verify password with BCrypt
            │  └─ If invalid → UnauthorizedException (401)
            │
            ├─ Generate JWT token
            │
            └─ Return AuthResponse (200 OK)
                 │
                 └─ Frontend stores JWT token
```

---

### Flow 3: OAuth Login (Google/GitHub)

```
┌─────────────────────────────────────────────────────────────────┐
│ OAuth 2.0 Login Flow (Google/GitHub)                            │
└─────────────────────────────────────────────────────────────────┘

1. FRONTEND INITIATES LOGIN
   ┌─────────────────────────────────────────────────────────────┐
   │ User clicks "Login with Google"                             │
   │                                                              │
   │ Frontend constructs authorization URL:                      │
   │   https://accounts.google.com/o/oauth2/v2/auth              │
   │     ?client_id=...                                          │
   │     &scope=email profile                                    │
   │     &response_type=code                                     │
   │     &redirect_uri=http://localhost:3000/callback            │
   │                                                              │
   │ Redirects user to Google                                    │
   └─────────────────────────────────────────────────────────────┘
                                │
                                ▼
   ┌─────────────────────────────────────────────────────────────┐
   │ GOOGLE AUTHORIZATION                                        │
   │                                                              │
   │ User logs into Google (if not already)                      │
   │ User grants permission to access profile                    │
   │                                                              │
   │ Google redirects back to frontend with code:                │
   │   http://localhost:3000/callback?code=AUTH_CODE             │
   └─────────────────────────────────────────────────────────────┘
                                │
                                ▼
   ┌─────────────────────────────────────────────────────────────┐
   │ FRONTEND EXCHANGES CODE FOR BACKEND HANDLING                │
   │                                                              │
   │ Frontend captures code from URL                             │
   │ Sends to backend:                                           │
   │   POST /api/v1/auth/oauth/callback/google                  │
   │   { "code": "AUTH_CODE" }                                   │
   └─────────────────────────────────────────────────────────────┘
                                │
                                ▼

2. BACKEND PROCESSES OAUTH CALLBACK
   ┌─────────────────────────────────────────────────────────────┐
   │ OAuthController.handleOAuthCallback(provider, request)      │
   │                                                              │
   │ Step 1: Validate provider (google/github)                   │
   │                                                              │
   │ Step 2: Exchange code for access token                      │
   │   → GoogleOAuthService.exchangeCodeForToken(code)           │
   │   → Makes POST to Google's token endpoint                   │
   │   → Returns: { access_token, token_type, expires_in }       │
   │                                                              │
   │ Step 3: Fetch user profile from provider                    │
   │   → GoogleOAuthService.getUserProfile(accessToken)          │
   │   → Makes GET to Google's userinfo endpoint                 │
   │   → Returns: { id, email, name, picture }                   │
   │                                                              │
   │ Step 4: Process user (create/update)                        │
   │   → OAuthUserProcessorService.processOAuthUser()            │
   │                                                              │
   │   ├─ Check if user exists with provider ID                 │
   │   │  └─ If yes: Return existing user                       │
   │   │                                                         │
   │   ├─ Check if email exists                                 │
   │   │  └─ If yes & different provider:                       │
   │   │     Throw AccountLinkingException                      │
   │   │                                                         │
   │   └─ If new: Create user                                   │
   │      Set email, username (from email)                      │
   │      Set oauth_provider = GOOGLE                           │
   │      Generate random password                              │
   │      Save to database                                      │
   │                                                             │
   │ Step 5: Generate JWT token                                 │
   │   → TokenService.generateToken(user)                       │
   │                                                             │
   │ Step 6: Return AuthResponse with JWT                       │
   └─────────────────────────────────────────────────────────────┘
                                │
                                ▼
   ┌─────────────────────────────────────────────────────────────┐
   │ FRONTEND COMPLETES LOGIN                                    │
   │                                                              │
   │ Frontend receives JWT token in response                     │
   │ Stores JWT in localStorage                                 │
   │ Redirects user to dashboard                                │
   │ Subsequent requests include JWT in Authorization header    │
   └─────────────────────────────────────────────────────────────┘
```

**Account Linking Scenario (Same Email, Different Provider):**

```
Existing User: john@example.com (Logged in with password)
                   │
                   └─ Tries to login with Google
                         │
                         └─ Google OAuth has same email
                              │
                              ▼
                    Account Linking Required
                         │
                         ├─ Throw AccountLinkingException
                         │
                         └─ Frontend shows:
                            "Email already exists"
                            "Link to existing account?"
                            │
                            └─ If user confirms:
                                POST /api/v1/auth/oauth/link/google
                                │
                                ├─ Create UserOAuthProvider record
                                ├─ Link Google ID to existing user
                                └─ Return AuthResponse
```

---

### Flow 4: JWT Token Usage in Protected Requests

```
┌─────────────────────────────────────────────────────────────────┐
│ JWT Token Usage in Protected API Requests                       │
└─────────────────────────────────────────────────────────────────┘

Frontend
  │
  ├─ Has JWT token in localStorage
  │
  └─ Makes request to protected endpoint:
       │
       │ GET /api/v1/databases
       │ Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
       │
       ▼
Spring Security Filter Chain
  │
  ├─ Extract Authorization header
  │
  ├─ Validate Bearer scheme
  │
  ├─ Extract JWT token
  │
  └─ JwtAuthenticationFilter.doFilterInternal()
       │
       ├─ Parse JWT (verify signature)
       │
       ├─ Check expiration
       │  └─ If expired → JwtException → 401 Unauthorized
       │
       ├─ Extract user ID from subject
       │
       ├─ Load User from database
       │
       ├─ Create Authentication object
       │  └─ SecurityContext.setAuthentication(auth)
       │
       └─ filterChain.doFilter() → Continue to controller
            │
            ▼
       Controller can access authentication info
            │
            └─ Principal.getName() = user ID
```

---

## 📡 API Endpoints

### Authentication Endpoints

#### 1. Register User
```
POST /api/v1/auth/register
Content-Type: application/json

Request:
{
  "email": "user@example.com",
  "username": "johndoe",
  "password": "SecurePass123!",
  "userType": "INDIVIDUAL"
}

Response (201 Created):
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "user": {
    "userId": 1,
    "email": "user@example.com",
    "username": "johndoe",
    "userType": "INDIVIDUAL",
    "status": "ACTIVE"
  },
  "message": "User registered successfully"
}

Error Responses:
- 409 Conflict: Email/username already exists
- 400 Bad Request: Validation failed
```

#### 2. Login User
```
POST /api/v1/auth/login
Content-Type: application/json

Request:
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}

Response (200 OK):
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "user": {
    "userId": 1,
    "email": "user@example.com",
    "username": "johndoe",
    "userType": "INDIVIDUAL",
    "status": "ACTIVE"
  },
  "message": "Login successful"
}

Error Responses:
- 404 Not Found: User not found
- 401 Unauthorized: Invalid password
```

#### 3. Get User Profile
```
GET /api/v1/auth/profile?userId=1
Authorization: Bearer {token}

Response (200 OK):
{
  "userId": 1,
  "email": "user@example.com",
  "username": "johndoe",
  "firstName": "John",
  "lastName": "Doe",
  "userType": "INDIVIDUAL",
  "status": "ACTIVE",
  "profilePictureUrl": null,
  "oauthProvider": null,
  "createdAt": "2025-11-18T10:30:00Z"
}

Error Responses:
- 404 Not Found: User not found
- 401 Unauthorized: Invalid token
```

---

### OAuth Endpoints

#### 1. OAuth Login/Callback
```
POST /api/v1/auth/oauth/callback/{provider}
Content-Type: application/json
{provider: google | github}

Request:
{
  "code": "4/0AY9c...auth-code...",
  "state": "csrf-protection-token",
  "redirectUri": "http://localhost:3000/callback"
}

Response (200 OK):
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "user": {
    "userId": 2,
    "email": "oauth@example.com",
    "username": "oauth_user",
    "userType": "INDIVIDUAL",
    "status": "ACTIVE",
    "oauthProvider": "GOOGLE"
  },
  "message": "OAuth authentication successful"
}

Error Responses:
- 400 Bad Request: Invalid code or provider
- 409 Conflict: Email exists with different provider (AccountLinkingException)
- 500 Internal Server Error: OAuth provider error
```

#### 2. Link OAuth Provider
```
POST /api/v1/auth/oauth/link/{provider}
Authorization: Bearer {token}
Content-Type: application/json
{provider: google | github}

Request:
{
  "code": "4/0AY9c...auth-code..."
}

Response (200 OK):
{
  "message": "OAuth provider ready for linking",
  "provider": "google",
  "email": "user@example.com"
}
```

---

### Plan Endpoints

#### 1. Get All Plans
```
GET /api/v1/plans
No authentication required

Response (200 OK):
[
  {
    "planId": 1,
    "name": "FREE",
    "description": "Free tier",
    "maxDatabases": 2,
    "price": 0.00,
    "billingCycle": "monthly"
  },
  {
    "planId": 2,
    "name": "STANDARD",
    "description": "Standard tier",
    "maxDatabases": 5,
    "price": 19.99,
    "billingCycle": "monthly"
  },
  ...
]
```

#### 2. Get Plan by ID
```
GET /api/v1/plans/{planId}
No authentication required

Response (200 OK):
{
  "planId": 1,
  "name": "FREE",
  "description": "Free tier",
  "maxDatabases": 2,
  "price": 0.00,
  "billingCycle": "monthly"
}

Error Responses:
- 404 Not Found: Plan not found
```

#### 3. Get Plan by Name
```
GET /api/v1/plans/name/{name}
No authentication required
{name: FREE | STANDARD | PREMIUM}

Response (200 OK):
{
  "planId": 1,
  "name": "FREE",
  ...
}
```

---

## 🔒 Security Configuration

### SecurityConfig Settings

**CORS Configuration:**
```java
- Allow origins: * (configurable per environment)
- Allow methods: GET, POST, PUT, DELETE, OPTIONS
- Allow headers: *, including Authorization
- Allow credentials: true
```

**Authentication Rules:**
```
Public Endpoints:
✓ POST   /api/v1/auth/register
✓ POST   /api/v1/auth/login
✓ POST   /api/v1/auth/oauth/callback/**
✓ GET    /api/v1/plans/**
✓ GET    /api/v1/health

Protected Endpoints (Require JWT):
✗ All other /api/v1/** routes
  └─ Return 401 Unauthorized if JWT missing/invalid
```

**Password Encoding:**
```
Algorithm: BCrypt
Strength: 12 rounds
Time: ~100ms per hash on modern hardware
```

**JWT Configuration:**
```properties
jwt.secret=${JWT_SECRET}  # Must be 256+ bits
jwt.expiration=86400000   # 24 hours in milliseconds
```

---

## 📊 Data Models

### User Entity
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(nullable = true)  // Null for OAuth users
    private String password;
    
    private String firstName;
    private String lastName;
    private String profilePictureUrl;
    
    @Column(unique = true)
    private String googleId;
    
    @Column(unique = true)
    private String githubId;
    
    @Enumerated(EnumType.STRING)
    private OAuthProvider oauthProvider;
    
    @Enumerated(EnumType.STRING)
    private UserType userType;
    
    @ManyToOne(fetch = FetchType.LAZY)
    private Plan personalPlan;
    
    @Enumerated(EnumType.STRING)
    private UserStatus status;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;  // Soft delete
}
```

### Plan Entity
```java
@Entity
@Table(name = "plan")
public class Plan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer planId;
    
    @Column(unique = true, nullable = false)
    private String name;
    
    private String description;
    
    @Column(nullable = false)
    private Integer maxDatabases;
    
    @Column(nullable = false)
    private BigDecimal price;
    
    private String billingCycle;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### UserOAuthProvider Entity
```java
@Entity
@Table(name = "user_oauth_providers")
public class UserOAuthProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer providerId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;
    
    @Enumerated(EnumType.STRING)
    private OAuthProvider provider;
    
    @Column(unique = true)
    private String providerUserId;
    
    private String providerEmail;
    private String providerName;
    
    private LocalDateTime linkedAt;
}
```

---

## ⚠️ Exception Handling

The Auth module uses custom exceptions mapped to HTTP status codes:

| Exception | HTTP Status | Scenario |
|-----------|-------------|----------|
| `ConflictException` | 409 Conflict | Email/username exists, account linking required |
| `UnauthorizedException` | 401 Unauthorized | Invalid password, expired JWT, missing auth |
| `ResourceNotFoundException` | 404 Not Found | User/plan not found |
| `AuthException` | 400 Bad Request | Invalid token format, JWT parsing error |
| `OAuthException` | 400 Bad Request | OAuth provider error |
| `AccountLinkingException` | 409 Conflict | Email exists with different OAuth provider |
| `JwtException` | 401 Unauthorized | Invalid/expired JWT token |

**Global Exception Handler:**
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<?> handleConflict(ConflictException e) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(e.getMessage()));
    }
    
    // ... other handlers
}
```

---

## 🔗 Integration Points

### With Database Module
- Uses `Plan` from common models to enforce database creation limits
- Links user to plan: `User.personalPlan`

### With Common Utilities
- Uses exceptions from `com.riwi.CrudCloud.common.util.exception.*`
- Uses models from `com.riwi.CrudCloud.common.models.*`

### With Frontend
- Provides JWT token for subsequent API calls
- Returns user information in standardized DTO format
- Supports OAuth provider integration

### With Payment Module (Future)
- Plan upgrade endpoints
- Payment processing
- Subscription management

---

## ⚙️ Configuration

### Required Environment Variables

```bash
# JWT Configuration
JWT_SECRET=your-256-bit-base64-encoded-secret-key-here
JWT_EXPIRATION=86400000

# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/crudcloud
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=password

# Google OAuth
GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-client-secret
GOOGLE_TOKEN_URL=https://oauth2.googleapis.com/token
GOOGLE_USER_INFO_URL=https://www.googleapis.com/oauth2/v2/userinfo

# GitHub OAuth
GITHUB_CLIENT_ID=your-client-id
GITHUB_CLIENT_SECRET=your-client-secret
GITHUB_TOKEN_URL=https://github.com/login/oauth/access_token
GITHUB_USER_INFO_URL=https://api.github.com/user
```

### Application Properties

```properties
# Server configuration
server.port=8080
server.servlet.context-path=/

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

# JWT
jwt.secret=${JWT_SECRET}
jwt.expiration=${JWT_EXPIRATION:86400000}

# Logging
logging.level.com.riwi.CrudCloud.auth=DEBUG
logging.level.org.springframework.security=DEBUG
```

---

## 📝 Summary

The Auth Module provides:

1. ✅ **Traditional Authentication:** Email/password registration and login
2. ✅ **OAuth 2.0 Support:** Google and GitHub authentication
3. ✅ **JWT Token Management:** Secure, stateless authentication
4. ✅ **Plan Management:** Subscription plan data and limits
5. ✅ **Security:** BCrypt hashing, JWT validation, Spring Security integration
6. ✅ **Account Linking:** Connect multiple OAuth providers to one account
7. ✅ **Error Handling:** Comprehensive exception handling with meaningful error messages

**Key Technologies:**
- Spring Security 6
- JWT (JSON Web Tokens)
- BCrypt password hashing
- OAuth 2.0 (Google & GitHub)
- Spring Data JPA
- Jakarta/Javax annotations

**Flow Summary:**
```
User Registration
    ↓
Email/Password or OAuth 2.0
    ↓
User Creation/Validation
    ↓
JWT Token Generation
    ↓
Frontend Stores Token
    ↓
Subsequent Requests Include JWT
    ↓
JwtAuthenticationFilter Validates
    ↓
Access to Protected Resources
```

---

**Document Status:** Complete  
**Last Updated:** November 18, 2025  
**Maintainer:** Development Team  
**Related Documents:**
- `REFACTOR_INSTANCES_TO_DATABASES.md` - Database terminology refactor
- `crudcloud_assignment.md` - Project specification
