# Auth Module - Database Mapping Documentation

## Overview
This document describes the database mapping structure created for the CrudCloud auth module based on the schema.sql specifications.

---

## Entity Models

### 1. **Plan** (`com.riwi.CrudCloud.auth.model.Plan`)
Maps to the `plan` table in the database.

**Key Features:**
- Auto-generated plan ID (primary key)
- Supports three plan types: FREE, STANDARD, PREMIUM
- Stores maximum instances allowed per plan
- Tracks pricing and billing cycle (monthly/yearly)
- Uses JPA timestamps for audit trail

**Relationships:**
- One-to-Many with User (users can have a personal plan)

**Example Usage:**
```java
Plan plan = Plan.builder()
    .name("PREMIUM")
    .maxInstances(10)
    .price(new BigDecimal("49.99"))
    .billingCycle("monthly")
    .build();
```

---

### 2. **User** (`com.riwi.CrudCloud.auth.model.User`)
Maps to the `users` table in the database.

**Key Features:**
- Auto-generated user ID (primary key)
- Unique email and username constraints
- Supports two user types: INDIVIDUAL, ORGANIZATIONAL_USER
- Status tracking: ACTIVE, INACTIVE
- Soft delete support via `deletedAt` field
- Lazy-loaded relationship with Plan

**Enums:**
- **UserType**: `INDIVIDUAL`, `ORGANIZATIONAL_USER`
- **UserStatus**: `ACTIVE`, `INACTIVE`

**Methods:**
- `delete()`: Soft delete the user (sets deletedAt and status to INACTIVE)
- `isDeleted()`: Check if user is soft-deleted

**Example Usage:**
```java
User user = User.builder()
    .username("john_doe")
    .email("john@example.com")
    .password("hashed_password")
    .userType(UserType.INDIVIDUAL)
    .status(UserStatus.ACTIVE)
    .build();
```

---

## Repositories

### 1. **PlanRepository** (`com.riwi.CrudCloud.auth.repository.PlanRepository`)
Handles database operations for Plan entities.

**Custom Query Methods:**
- `findByName(String name)`: Find plan by name

### 2. **UserRepository** (`com.riwi.CrudCloud.auth.repository.UserRepository`)
Handles database operations for User entities with comprehensive query methods.

**Custom Query Methods:**
- `findByEmail(String email)`: Find user by email
- `findByUsername(String username)`: Find user by username
- `findActiveUsersByType(UserType userType)`: Get all active users of a specific type
- `findUsersByPersonalPlanId(Integer planId)`: Find all users assigned to a plan
- `existsByEmailAndNotDeleted(String email)`: Check if email exists (excluding soft-deleted)
- `existsByUsernameAndNotDeleted(String username)`: Check if username exists (excluding soft-deleted)

**Features:**
- All queries respect soft delete logic (filter by `deletedAt IS NULL`)
- LAZY loading enabled for relationships

---

## DTOs (Data Transfer Objects)

### Request DTOs

#### **RegisterRequest** (`com.riwi.CrudCloud.auth.dto.request.RegisterRequest`)
```java
{
  "email": "user@example.com",
  "username": "john_doe",
  "password": "Secure1!Password",
  "userType": "INDIVIDUAL"
}
```

**Validations:**
- Email: Valid format, required
- Username: 3-50 characters, required
- Password: Min 8 chars, 1 uppercase, 1 number, 1 special character
- User Type: INDIVIDUAL or ORGANIZATIONAL_USER

#### **LoginRequest** (`com.riwi.CrudCloud.auth.dto.request.LoginRequest`)
```java
{
  "email": "user@example.com",
  "password": "Secure1!Password"
}
```

**Validations:**
- Email: Valid format, required
- Password: Required

---

### Response DTOs

#### **UserResponse** (`com.riwi.CrudCloud.auth.dto.response.UserResponse`)
```java
{
  "userId": 1,
  "username": "john_doe",
  "email": "user@example.com",
  "userType": "INDIVIDUAL",
  "status": "ACTIVE",
  "createdAt": "2025-11-10T10:30:00"
}
```

#### **AuthResponse** (`com.riwi.CrudCloud.auth.dto.response.AuthResponse`)
```java
{
  "token": "jwt-token-here",
  "user": { UserResponse object }
}
```

#### **PlanResponse** (`com.riwi.CrudCloud.auth.dto.response.PlanResponse`)
```java
{
  "planId": 1,
  "name": "PREMIUM",
  "description": "Premium plan with advanced features",
  "maxInstances": 10,
  "price": 49.99,
  "billingCycle": "monthly"
}
```

---

## Services

### 1. **AuthService** (`com.riwi.CrudCloud.auth.service.AuthService`)
Handles user authentication and profile management.

**Key Methods:**

#### `register(RegisterRequest)`
- Validates email and username uniqueness
- Creates new user with ACTIVE status
- **TODO**: Hash password with BCrypt
- **TODO**: Generate JWT token
- Returns `AuthResponse`

#### `login(LoginRequest)`
- Finds user by email
- **TODO**: Verify password with BCrypt
- **TODO**: Generate JWT token
- Returns `AuthResponse` or throws `InvalidCredentialsException`

#### `getUserProfile(Integer userId)`
- Retrieves user by ID
- Returns `UserResponse`
- Throws `UserNotFoundException` if not found

#### `validateToken(String token)`
- **TODO**: Implement JWT validation

#### `extractUserIdFromToken(String token)`
- **TODO**: Extract user ID from JWT token

---

### 2. **PlanService** (`com.riwi.CrudCloud.auth.service.PlanService`)
Manages plan queries and retrieval.

**Key Methods:**

#### `getAllPlans()`
- Returns list of all available plans as `List<PlanResponse>`

#### `getPlanById(Integer planId)`
- Returns single plan by ID
- Throws `AuthException` if not found

#### `getPlanByName(String name)`
- Returns single plan by name
- Throws `AuthException` if not found

---

## Controllers

### 1. **AuthController** (`com.riwi.CrudCloud.auth.controller.AuthController`)

**Endpoints:**

| Method | Endpoint | Request | Response | Status |
|--------|----------|---------|----------|--------|
| POST | `/api/auth/register` | `RegisterRequest` | `AuthResponse` | 201 Created |
| POST | `/api/auth/login` | `LoginRequest` | `AuthResponse` | 200 OK |
| GET | `/api/auth/profile` | userId (param) | `UserResponse` | 200 OK |

---

### 2. **PlanController** (`com.riwi.CrudCloud.auth.controller.PlanController`)

**Endpoints:**

| Method | Endpoint | Response | Status |
|--------|----------|----------|--------|
| GET | `/api/plans` | `List<PlanResponse>` | 200 OK |
| GET | `/api/plans/{planId}` | `PlanResponse` | 200 OK |
| GET | `/api/plans/name/{name}` | `PlanResponse` | 200 OK |

---

## Exception Handling

### Exception Hierarchy
```
AuthException (base)
├── UserAlreadyExistsException
├── UserNotFoundException
└── InvalidCredentialsException
```

### Global Exception Handler (`GlobalExceptionHandler`)
Provides centralized error handling with standardized responses.

**Error Response Format:**
```java
{
  "code": "ERROR_CODE",
  "message": "Human-readable message",
  "details": { /* optional additional info */ },
  "timestamp": "2025-11-10T10:30:00"
}
```

**HTTP Status Codes:**
- 201: Resource created (register success)
- 200: Success (login, get profile/plans)
- 400: Validation error or general auth error
- 401: Invalid credentials
- 404: User/Plan not found
- 409: User already exists (conflict)
- 500: Internal server error

---

## Database Indexes
The schema includes optimized indexes for common queries:

**User Indexes:**
- `idx_users_deleted_at`: Soft delete queries
- `idx_users_email`: Login queries
- `idx_users_username`: Username lookup
- `idx_users_type_status`: User type + active status
- `idx_users_personal_plan_id`: Plan queries

**Plan Indexes:**
- `idx_plan_name`: Plan name searches

---

## Transaction Management

### Transactional Annotations:
- `AuthService.register()`: `@Transactional` (write operation)
- `AuthService.login()`: `@Transactional(readOnly = true)` (read-only)
- `AuthService.getUserProfile()`: `@Transactional(readOnly = true)`
- `PlanService` methods: `@Transactional(readOnly = true)`

---

## TODO Items for Production

1. **Password Hashing**
   - Implement BCrypt password hashing
   - Update `AuthService.register()` and `AuthService.login()`
   - Add Spring Security dependency

2. **JWT Token Generation**
   - Implement JWT token generation in `AuthService.register()` and `AuthService.login()`
   - Implement JWT validation in `AuthService.validateToken()`
   - Implement token extraction in `AuthService.extractUserIdFromToken()`
   - Add JWT library to dependencies (e.g., jjwt)

3. **Security Configuration**
   - Create `SecurityConfig` class for Spring Security
   - Add JWT filter for request authentication
   - Configure CORS properly

4. **Database Configuration**
   - Configure database connection in `application.properties`
   - Add database driver dependency to `pom.xml`

5. **User Profile Endpoint**
   - Update `AuthController.getProfile()` to extract userId from JWT instead of request param

---

## Directory Structure

```
src/main/java/com/riwi/CrudCloud/auth/
├── config/
│   └── GlobalExceptionHandler.java
├── controller/
│   ├── AuthController.java
│   └── PlanController.java
├── dto/
│   ├── request/
│   │   ├── LoginRequest.java
│   │   └── RegisterRequest.java
│   └── response/
│       ├── AuthResponse.java
│       ├── PlanResponse.java
│       └── UserResponse.java
├── exception/
│   ├── AuthException.java
│   ├── InvalidCredentialsException.java
│   ├── UserAlreadyExistsException.java
│   └── UserNotFoundException.java
├── model/
│   ├── Plan.java
│   ├── User.java
│   ├── UserStatus.java
│   └── UserType.java
├── repository/
│   ├── PlanRepository.java
│   └── UserRepository.java
└── service/
    ├── AuthService.java
    └── PlanService.java
```

---

## Notes

- All entities use `@Enumerated(EnumType.STRING)` for enum fields
- Lazy loading is configured for the Plan relationship in User
- Soft delete is implemented using `deletedAt` timestamp
- All repositories include proper javadoc comments
- CORS is enabled on controllers for frontend integration
- All endpoints use proper HTTP status codes
- Input validation is performed using Jakarta Bean Validation

