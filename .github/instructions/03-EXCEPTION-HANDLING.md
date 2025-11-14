# ⚠️ Exception Handling Strategy

## Overview

This document explains the exception handling architecture used in the CrudCloud application. The system implements a **layered, category-based exception handling pattern** using Spring's `@ControllerAdvice` annotation combined with specialized handler classes.

All custom exceptions extend `RuntimeException`, making them unchecked exceptions. This is a best practice in Spring Boot as it allows for cleaner code without requiring try-catch blocks.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                   Global Exception Flow                      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                   ┌──────────────────────┐
                   │ GlobalExceptionHandler │
                   │   (@ControllerAdvice) │
                   └──────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ Validation       │  │ Authentication   │  │ Resource (404)   │
│ (400)            │  │ (401, 403)       │  │ NotFound         │
│ BadRequest       │  │ Unauthorized     │  │                  │
│ MethodArgument   │  │ Forbidden        │  │                  │
│ NotValid         │  │                  │  │                  │
└──────────────────┘  └──────────────────┘  └──────────────────┘
        │                     │                     │
        ▼                     ▼                     ▼
┌──────────────────┐  ┌──────────────────┐
│ Business Logic   │  │ System (500)     │
│ (409, 422)       │  │ Database         │
│ Conflict         │  │ General Error    │
│ Unprocessable    │  │                  │
│ Entity           │  │                  │
└──────────────────┘  └──────────────────┘
```

---

## Project Structure

```
auth/
├── config/
├── controller/
├── dto/
├── model/
├── repository/
├── service/
│   ├── AuthService.java
│   └── PlanService.java
└── util/
    └── exception/
        ├── ApplicationException.java (base class)
        ├── ClientErrorException.java (4xx errors)
        ├── ServerErrorException.java (5xx errors)
        ├── GlobalExceptionHandler.java
        ├── classes/
        │   ├── AuthException.java (legacy, 401)
        │   ├── BadRequestException.java (400)
        │   ├── ConflictException.java (409)
        │   ├── DatabaseException.java (500)
        │   ├── ForbiddenException.java (403)
        │   ├── ResourceNotFoundException.java (404)
        │   ├── UnauthorizedException.java (401)
        │   └── UnprocessableEntityException.java (422)
        ├── handlers/
        │   ├── AuthenticationExceptionHandler.java
        │   ├── BusinessLogicExceptionHandler.java
        │   ├── ResourceExceptionHandler.java
        │   ├── SystemExceptionHandler.java
        │   └── ValidationExceptionHandler.java
        └── dto/
            └── ErrorResponse.java
```

---

## Exception Hierarchy

The application uses a **three-tier exception hierarchy** for organized and type-safe error handling:

```
RuntimeException
    └── ApplicationException (base class for all custom exceptions)
        ├── ClientErrorException (4xx client errors)
        │   ├── BadRequestException (400)
        │   ├── UnauthorizedException (401)
        │   ├── ForbiddenException (403)
        │   ├── ResourceNotFoundException (404)
        │   ├── ConflictException (409)
        │   ├── UnprocessableEntityException (422)
        │   └── AuthException (401 - legacy)
        │
        └── ServerErrorException (5xx server errors)
            └── DatabaseException (500)
```

### Hierarchy Benefits

**1. Type-Safe Exception Catching:**
```java
// Catch all 4xx client errors
try {
    operation();
} catch (ClientErrorException e) {
    handleClientError(e);
}

// Catch all 5xx server errors
catch (ServerErrorException e) {
    handleServerError(e);
}

// Catch any application exception
catch (ApplicationException e) {
    int httpStatus = e.getHttpStatus();
    handleApplicationError(e);
}
```

**2. Built-In HTTP Status Codes:**
Each exception automatically carries its HTTP status code via the `getHttpStatus()` method from `ApplicationException`:
```java
BadRequestException e = new BadRequestException("Invalid input");
e.getHttpStatus();  // Returns 400

DatabaseException e = new DatabaseException("Connection failed", cause);
e.getHttpStatus();  // Returns 500
```

**3. Semantic Organization:**
- **ClientErrorException (4xx):** Client made an error - don't retry
- **ServerErrorException (5xx):** Server problem - can retry with backoff

---

## Custom Exceptions

### 1. **BadRequestException** (HTTP 400)

```java
public class BadRequestException extends ClientErrorException {
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST.value(), message);
    }
    
    public BadRequestException(String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST.value(), message, cause);
    }
}
```

**Purpose:** Thrown when a request contains syntactically invalid data.

**HTTP Status:** 400 Bad Request

**Hierarchy:** `RuntimeException` → `ApplicationException` → `ClientErrorException` → `BadRequestException`

**When to Use:**
- Missing required fields
- Invalid data types
- Malformed request syntax

**Example:**
```java
if (email == null || email.isEmpty()) {
    throw new BadRequestException("Email is required");
}
```

**Can be caught as:**
```java
catch (BadRequestException e) { }         // Specific
catch (ClientErrorException e) { }        // Category (4xx)
catch (ApplicationException e) { }        // Any custom exception
```

---

### 2. **UnauthorizedException** (HTTP 401)

```java
public class UnauthorizedException extends ClientErrorException {
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED.value(), message);
    }
    
    public UnauthorizedException(String message, Throwable cause) {
        super(HttpStatus.UNAUTHORIZED.value(), message, cause);
    }
}
```

**Purpose:** Thrown when authentication fails (invalid credentials).

**HTTP Status:** 401 Unauthorized

**Hierarchy:** `RuntimeException` → `ApplicationException` → `ClientErrorException` → `UnauthorizedException`

**Key Distinction:**
- **401 Unauthorized** = "I don't know who you are" or "Invalid credentials"
- **403 Forbidden** = "I know who you are, but you can't do that"

**When to Use:**
- Wrong password provided
- Invalid authentication token
- User not found during login

**Example:**
```java
if (!validPassword(user, password)) {
    throw new UnauthorizedException("Invalid credentials");
}
```

**Can be caught as:**
```java
catch (UnauthorizedException e) { }       // Specific
catch (ClientErrorException e) { }        // Category (4xx)
catch (ApplicationException e) { }        // Any custom exception
```

---

### 3. **ForbiddenException** (HTTP 403)

```java
public class ForbiddenException extends ClientErrorException {
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN.value(), message);
    }
    
    public ForbiddenException(String message, Throwable cause) {
        super(HttpStatus.FORBIDDEN.value(), message, cause);
    }
}
```

**Purpose:** Thrown when a user lacks authorization to access a resource.

**HTTP Status:** 403 Forbidden

**Hierarchy:** `RuntimeException` → `ApplicationException` → `ClientErrorException` → `ForbiddenException`

**When to Use:**
- User tries to delete a resource they don't own
- User attempts to access restricted resources
- User lacks proper permissions for an operation

**Example:**
```java
if (!resource.getOwnerId().equals(currentUserId)) {
    throw new ForbiddenException("You do not have permission to access this resource");
}
```

**Can be caught as:**
```java
catch (ForbiddenException e) { }          // Specific
catch (ClientErrorException e) { }        // Category (4xx)
catch (ApplicationException e) { }        // Any custom exception
```

---

### 4. **ResourceNotFoundException** (HTTP 404)

```java
public class ResourceNotFoundException extends ClientErrorException {
    
    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND.value(), message);
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super(HttpStatus.NOT_FOUND.value(), message, cause);
    }
    
    /**
     * Factory method for easier resource not found messages
     */
    public static ResourceNotFoundException withResource(String resourceType, String field, Object value) {
        return new ResourceNotFoundException(
            String.format("%s not found with %s: %s", resourceType, field, value)
        );
    }
    
    /**
     * Factory method for ID-based lookups
     */
    public static ResourceNotFoundException withId(String resourceType, Object id) {
        return new ResourceNotFoundException(
            String.format("%s not found with ID: %s", resourceType, id)
        );
    }
}
```

**Purpose:** Thrown when a requested resource cannot be found in the database.

**HTTP Status:** 404 Not Found

**Hierarchy:** `RuntimeException` → `ApplicationException` → `ClientErrorException` → `ResourceNotFoundException`

**Factory Methods:**
- `ResourceNotFoundException.withId(resourceType, id)` - For ID-based lookups
- `ResourceNotFoundException.withResource(resourceType, field, value)` - For field-based lookups

**When to Use:**
- Project/Plan ID doesn't exist
- User requests a non-existent resource
- Database query returns no results

**Examples:**
```java
// Using factory methods
return repository.findById(id)
    .orElseThrow(() -> ResourceNotFoundException.withId("User", id));

// Or using withResource
throw ResourceNotFoundException.withResource("Project", "slug", "invalid-slug");

// Or direct message
throw new ResourceNotFoundException("User with ID 999 not found");
```

**Can be caught as:**
```java
catch (ResourceNotFoundException e) { }   // Specific
catch (ClientErrorException e) { }        // Category (4xx)
catch (ApplicationException e) { }        // Any custom exception
```

---

### 5. **ConflictException** (HTTP 409)

```java
public class ConflictException extends ClientErrorException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT.value(), message);
    }
    
    public ConflictException(String message, Throwable cause) {
        super(HttpStatus.CONFLICT.value(), message, cause);
    }
}
```

**Purpose:** Thrown when a request conflicts with the current state of the resource or violates uniqueness constraints.

**HTTP Status:** 409 Conflict

**Hierarchy:** `RuntimeException` → `ApplicationException` → `ClientErrorException` → `ConflictException`

**When to Use:**
- State machine violations (e.g., transitioning COMPLETED → PENDING)
- Duplicate email/username during registration
- Resource state prevents the operation
- Uniqueness constraint violations

**Example:**
```java
if (userRepository.existsByEmail(email)) {
    throw new ConflictException("User with this email already exists");
}
```

**Can be caught as:**
```java
catch (ConflictException e) { }           // Specific
catch (ClientErrorException e) { }        // Category (4xx)
catch (ApplicationException e) { }        // Any custom exception
```

---

### 6. **UnprocessableEntityException** (HTTP 422)

```java
public class UnprocessableEntityException extends ClientErrorException {
    public UnprocessableEntityException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY.value(), message);
    }
    
    public UnprocessableEntityException(String message, Throwable cause) {
        super(HttpStatus.UNPROCESSABLE_ENTITY.value(), message, cause);
    }
}
```

**Purpose:** Thrown when the request is syntactically valid but semantically incorrect.

**HTTP Status:** 422 Unprocessable Entity

**Hierarchy:** `RuntimeException` → `ApplicationException` → `ClientErrorException` → `UnprocessableEntityException`

**Key Distinction from BadRequestException:**
- **400 (BadRequest):** Syntax is invalid (JSON doesn't parse, missing required fields)
- **422 (UnprocessableEntity):** Syntax is valid, but violates business rules or semantic validation

**When to Use:**
- Request JSON is valid, but contains restricted keywords
- Values are syntactically correct but violate business logic
- Semantic validation fails

**Example:**
```java
List<String> restrictedKeywords = Arrays.asList("admin", "system", "root");
if (restrictedKeywords.contains(projectName.toLowerCase())) {
    throw new UnprocessableEntityException("Project name contains restricted keywords");
}
```

**Can be caught as:**
```java
catch (UnprocessableEntityException e) { } // Specific
catch (ClientErrorException e) { }         // Category (4xx)
catch (ApplicationException e) { }         // Any custom exception
```

---

### 7. **DatabaseException** (HTTP 500)

```java
public class DatabaseException extends ServerErrorException {
    public DatabaseException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
    }
    
    public DatabaseException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, cause);
    }
}
```

**Purpose:** Thrown when database operations fail unexpectedly.

**HTTP Status:** 500 Internal Server Error

**Hierarchy:** `RuntimeException` → `ApplicationException` → `ServerErrorException` → `DatabaseException`

**When to Use:**
- Database connection failures
- Unexpected database errors
- Transaction rollbacks

**Example:**
```java
try {
    repository.save(entity);
} catch (DataAccessException ex) {
    throw new DatabaseException("Failed to save entity", ex);
}
```

**Can be caught as:**
```java
catch (DatabaseException e) { }           // Specific
catch (ServerErrorException e) { }        // Category (5xx)
catch (ApplicationException e) { }        // Any custom exception
```

---

### 8. **AuthException** (HTTP 401 - Legacy)

```java
public class AuthException extends ClientErrorException {

    private String code;

    public AuthException(String message) {
        super(HttpStatus.UNAUTHORIZED.value(), message);
        this.code = "AUTH_ERROR";
    }

    public AuthException(String message, String code) {
        super(HttpStatus.UNAUTHORIZED.value(), message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
```

**Purpose:** Legacy authentication exception maintained for backward compatibility.

**HTTP Status:** 401 Unauthorized

**Hierarchy:** `RuntimeException` → `ApplicationException` → `ClientErrorException` → `AuthException`

**Status:** ⚠️ Deprecated - Use `UnauthorizedException` for new code

**Note:** This class is maintained for backward compatibility with existing code. Prefer using `UnauthorizedException` in new implementations.

**Can be caught as:**
```java
catch (AuthException e) { }               // Legacy specific
catch (ClientErrorException e) { }        // Category (4xx)
catch (ApplicationException e) { }        // Any custom exception
```

---

## Exception Handlers

### Handler Pattern

Each handler class is annotated with `@RestControllerAdvice` and manages a specific category of exceptions with appropriate HTTP status codes. Handlers implement the following pattern:

```java
@RestControllerAdvice
public class SpecificExceptionHandler {
    
    @ExceptionHandler(CustomException.class)
    @ResponseStatus(HttpStatus.XXX)
    public ResponseEntity<ErrorResponse> handleException(CustomException ex) {
        ErrorResponse response = new ErrorResponse(
            HttpStatus.XXX.value(),
            ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.XXX);
    }
}
```

**Note:** Each handler is independently annotated with `@RestControllerAdvice`, and the `GlobalExceptionHandler` orchestrator simply imports them via `@ControllerAdvice` with `basePackageClasses`.

### 1. **ValidationExceptionHandler** (HTTP 400)

Located in `auth/util/exception/handlers/ValidationExceptionHandler.java`

```java
@RestControllerAdvice
public class ValidationExceptionHandler {
    
    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException ex) {
        ErrorResponse response = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ErrorResponse response = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed",
            errors
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
```

**Handles:**
- `BadRequestException` → HTTP 400
- `MethodArgumentNotValidException` → HTTP 400 (Spring validation failures)

**Responsibilities:**
- Validates request syntax
- Handles Spring's built-in validation failures
- Provides detailed field-level error messages

**Response Format for BadRequestException:**
```json
{
  "status": 400,
  "message": "Email is required",
  "timestamp": "2025-11-13T10:30:00"
}
```

**Response Format for MethodArgumentNotValidException:**
```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "email": "Email is required",
    "password": "Password must be at least 8 characters"
  },
  "timestamp": "2025-11-13T10:30:00"
}
```

---

### 2. **AuthenticationExceptionHandler** (HTTP 401 & 403)

Located in `auth/util/exception/handlers/AuthenticationExceptionHandler.java`

```java
@RestControllerAdvice
public class AuthenticationExceptionHandler {
    
    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(
            UnauthorizedException ex) {
        
        ErrorResponse response = new ErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }
    
    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<ErrorResponse> handleForbiddenException(
            ForbiddenException ex) {
        
        ErrorResponse response = new ErrorResponse(
            HttpStatus.FORBIDDEN.value(),
            ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }
}
```

**Handles:**
- `UnauthorizedException` → HTTP 401
- `ForbiddenException` → HTTP 403

**Responsibilities:**
- Authentication failure detection
- Authorization checks
- Permission validation
- Access control enforcement

**Key Distinction:**
- **401 Unauthorized** = "I don't know who you are" OR "Invalid credentials"
- **403 Forbidden** = "I know who you are, but you can't do that"

**Response Format (401):**
```json
{
  "status": 401,
  "message": "Invalid credentials",
  "timestamp": "2025-11-13T10:30:00"
}
```

**Response Format (403):**
```json
{
  "status": 403,
  "message": "You do not have permission to access this resource",
  "timestamp": "2025-11-13T10:30:00"
}
```

---

### 3. **ResourceExceptionHandler** (HTTP 404)

Located in `auth/util/exception/handlers/ResourceExceptionHandler.java`

```java
@RestControllerAdvice
public class ResourceExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex) {
        
        ErrorResponse response = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
}
```

**Handles:**
- `ResourceNotFoundException` → HTTP 404

**Responsibilities:**
- Resource lookup failures
- Entity not found scenarios
- Missing resource errors

**Response Format:**
```json
{
  "status": 404,
  "message": "User not found with ID: 999",
  "timestamp": "2025-11-13T10:30:00"
}
```

---

### 4. **BusinessLogicExceptionHandler** (HTTP 409 & 422)

Located in `auth/util/exception/handlers/BusinessLogicExceptionHandler.java`

```java
@RestControllerAdvice
public class BusinessLogicExceptionHandler {
    
    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleConflictException(
            ConflictException ex) {
        
        ErrorResponse response = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }
    
    @ExceptionHandler(UnprocessableEntityException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ResponseEntity<ErrorResponse> handleUnprocessableEntityException(
            UnprocessableEntityException ex) {
        
        ErrorResponse response = new ErrorResponse(
            HttpStatus.UNPROCESSABLE_ENTITY.value(),
            ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
```

**Handles:**
- `ConflictException` → HTTP 409
- `UnprocessableEntityException` → HTTP 422

**Responsibilities:**
- Business rule validation
- State machine enforcement
- Semantic validation
- Uniqueness constraint checking

**Response Format (409):**
```json
{
  "status": 409,
  "message": "User with this email already exists",
  "timestamp": "2025-11-13T10:30:00"
}
```

**Response Format (422):**
```json
{
  "status": 422,
  "message": "Project name contains restricted keywords",
  "timestamp": "2025-11-13T10:30:00"
}
```

---

### 5. **SystemExceptionHandler** (HTTP 500)

Located in `auth/util/exception/handlers/SystemExceptionHandler.java`

```java
@RestControllerAdvice
public class SystemExceptionHandler {
    
    @ExceptionHandler(DatabaseException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleDatabaseException(
            DatabaseException ex) {
        
        // Log the cause for debugging
        logger.error("Database operation failed", ex);
        
        ErrorResponse response = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        
        // Log the unexpected error with full stack trace
        logger.error("Unexpected error", ex);
        
        ErrorResponse response = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An unexpected error occurred"
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```

**Handles:**
- `DatabaseException` → HTTP 500
- `Exception` (generic fallback) → HTTP 500

**Responsibilities:**
- Catch-all error handling
- Database failure management
- Unexpected error handling
- Infrastructure failures

**Response Format:**
```json
{
  "status": 500,
  "message": "Database operation failed",
  "timestamp": "2025-11-13T10:30:00"
}
```

---

### GlobalExceptionHandler (Orchestrator)

Located in `auth/util/exception/GlobalExceptionHandler.java`:

```java
@ControllerAdvice(
    basePackageClasses = {
        ValidationExceptionHandler.class,
        ResourceExceptionHandler.class,
        AuthenticationExceptionHandler.class,
        BusinessLogicExceptionHandler.class,
        SystemExceptionHandler.class
    }
)
public class GlobalExceptionHandler {
    // Orchestrator - delegates to specialized handlers
}
```

**Role:** 
- Acts as the central registration point for all exception handlers
- Uses `@ControllerAdvice` with `basePackageClasses` to register the application with Spring
- Orchestrates specialized handler classes by importing them

**Architecture:**
Each handler class is independently annotated with `@RestControllerAdvice`, and the `GlobalExceptionHandler` simply aggregates them using `@ControllerAdvice`. This allows:

```
GlobalExceptionHandler (@ControllerAdvice)
    ├── ValidationExceptionHandler (@RestControllerAdvice)
    ├── ResourceExceptionHandler (@RestControllerAdvice)
    ├── AuthenticationExceptionHandler (@RestControllerAdvice)
    ├── BusinessLogicExceptionHandler (@RestControllerAdvice)
    └── SystemExceptionHandler (@RestControllerAdvice)
```

**Benefits of This Architecture:**
- ✅ **Separation of Concerns:** Each handler manages one category of exceptions
- ✅ **Maintainability:** Easy to locate and modify specific exception handling
- ✅ **Scalability:** New handlers can be added without modifying existing code
- ✅ **Single Responsibility:** Each handler focuses on one task
- ✅ **Reusability:** Handler patterns can be copied for new exceptions
- ✅ **Modularity:** Handlers can be tested independently

---

### Error Response DTO

Located in `auth/util/exception/dto/ErrorResponse.java`:

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    private int status;                    // HTTP status code
    private String message;                // Human-readable message
    private LocalDateTime timestamp;       // When error occurred (set automatically)
    private Map<String, String> errors;    // Optional field-level errors
    private String path;                   // Optional request path
    
    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
    
    public ErrorResponse(int status, String message, LocalDateTime timestamp) {
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
    }
    
    public ErrorResponse(int status, String message, Map<String, String> errors) {
        this.status = status;
        this.message = message;
        this.errors = errors;
        this.timestamp = LocalDateTime.now();
    }
    
    public ErrorResponse(int status, String message, String path) {
        this.status = status;
        this.message = message;
        this.path = path;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and setters...
}
```

**Fields:**
- `status` - HTTP status code (400, 401, 403, 404, 409, 422, 500)
- `message` - Clear, user-friendly error message
- `timestamp` - ISO 8601 formatted timestamp (set automatically)
- `errors` - Optional map of field-level validation errors (only for 400 validation errors)
- `path` - Optional request path that caused the error

**Annotation:** `@JsonInclude(JsonInclude.Include.NON_NULL)` ensures null fields are not included in JSON response

**Usage in Handlers:**
```java
// Simple error response
ErrorResponse response = new ErrorResponse(
    HttpStatus.NOT_FOUND.value(),
    "User not found"
);

// With field-level errors
ErrorResponse response = new ErrorResponse(
    HttpStatus.BAD_REQUEST.value(),
    "Validation failed",
    fieldErrors  // Map<String, String>
);
```

---

## HTTP Status Codes Reference

| Status Code | Exception | Use Case |
|---|---|---|
| **200** | N/A | Successful request (reads, updates, authentication) |
| **201** | N/A | Resource successfully created (POST operations) |
| **400** | `BadRequestException` | Invalid request syntax or missing required fields |
| **401** | `UnauthorizedException` | Authentication failure (invalid credentials) |
| **403** | `ForbiddenException` | User lacks authorization for the resource |
| **404** | `ResourceNotFoundException` | Resource doesn't exist |
| **409** | `ConflictException` | Request conflicts with resource state or uniqueness violation |
| **422** | `UnprocessableEntityException` | Valid syntax, but semantic/business rule violation |
| **500** | `DatabaseException` | Database operation failed |
| **500** | `Exception` (generic) | Unexpected server error |

---

## Best Practices

### ✅ DO:

1. **Throw specific exceptions** - Use the most specific exception type for the situation
   ```java
   // Good
   throw new ResourceNotFoundException("User with ID 999 not found");
   
   // Bad - too generic
   throw new RuntimeException("Error occurred");
   ```

2. **Provide meaningful error messages** - Include context in the message
   ```java
   // Good
   throw new BadRequestException("Email is required");
   
   // Bad - unclear
   throw new BadRequestException("Invalid input");
   ```

3. **Use factory methods** - Leverage ResourceNotFoundException factory methods for consistency
   ```java
   // Good
   throw ResourceNotFoundException.withId("User", userId);
   
   // Also good
   throw new ResourceNotFoundException("User with ID " + userId + " not found");
   ```

4. **Preserve the original exception** - Pass the cause when wrapping exceptions
   ```java
   // Good
   try {
       repository.save(entity);
   } catch (DataAccessException ex) {
       throw new DatabaseException("Failed to save entity", ex);
   }
   ```

5. **Validate early** - Check constraints as soon as possible
   ```java
   // Good - validate at service layer
   public void registerUser(RegisterRequest request) {
       if (request.getEmail() == null) {
           throw new BadRequestException("Email is required");
       }
       if (userRepository.existsByEmail(request.getEmail())) {
           throw new ConflictException("Email already registered");
       }
       // Continue with registration...
   }
   ```

6. **Return correct HTTP status for success** - Use 201 Created for POST operations
   ```java
   // Good
   @PostMapping("/register")
   public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
       AuthResponse response = authService.register(request);
       return ResponseEntity.status(HttpStatus.CREATED).body(response);
   }
   ```

### ❌ DON'T:

1. **Catch and ignore exceptions** - Handle them properly or let them propagate
   ```java
   // Bad - silently swallowing exceptions
   try {
       service.registerUser(request);
   } catch (Exception e) {
       // Do nothing
   }
   
   // Good - let handler deal with it
   service.registerUser(request);
   
   // Or - explicitly handle if necessary
   try {
       service.registerUser(request);
   } catch (ConflictException e) {
       logger.warn("User already exists: {}", e.getMessage());
       throw e;  // Let controller handler process
   }
   ```

2. **Use generic Exception** - Be specific
   ```java
   // Bad
   if (condition) throw new Exception("Error occurred");
   
   // Good
   if (condition) throw new ConflictException("State conflict: resource cannot be modified");
   ```

3. **Swallow stack traces** - Preserve debugging information
   ```java
   // Bad
   catch (SQLException e) {
       throw new RuntimeException(e.toString());
   }
   
   // Good
   catch (SQLException e) {
       logger.error("Database error", e);
       throw new DatabaseException("Database operation failed", e);
   }
   ```

4. **Expose sensitive data** - Keep error messages user-friendly
   ```java
   // Bad - exposes database credentials
   catch (SQLException e) {
       throw new RuntimeException("Connection to jdbc:mysql://user:pass@host failed");
   }
   
   // Good - generic message, log details separately
   catch (SQLException e) {
       logger.error("Database connection failed: {}", e.getMessage(), e);
       throw new DatabaseException("Database operation failed", e);
   }
   ```

5. **Mix exception handling layers** - Keep concerns separated
   ```java
   // Bad - catching in controller
   @GetMapping("/{id}")
   public UserResponse getUser(@PathVariable Integer id) {
       try {
           return service.getUserById(id);
       } catch (ResourceNotFoundException ex) {
           return null;
       }
   }
   
   // Good - let handler deal with it
   @GetMapping("/{id}")
   public UserResponse getUser(@PathVariable Integer id) {
       return service.getUserById(id);  // Handler catches ResourceNotFoundException
   }
   ```

6. **Return 200 OK for POST operations** - Return 201 Created instead
   ```java
   // Bad
   @PostMapping("/create")
   public ResponseEntity<Entity> create(@RequestBody CreateRequest request) {
       return ResponseEntity.ok(service.create(request));
   }
   
   // Good
   @PostMapping("/create")
   public ResponseEntity<Entity> create(@RequestBody CreateRequest request) {
       return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
   }
   ```

---

## Exception Flow Examples

### Example 1: User Registration with Validation Failure

```
POST /api/auth/register with invalid email
       ↓
Spring @Valid annotation detects violation
       ↓
MethodArgumentNotValidException thrown
       ↓
ValidationExceptionHandler.handleValidationException()
       ↓
Returns 400 Bad Request with field errors
```

**Response (400):**
```json
{
  "status": 400,
  "message": "Validation failed",
  "timestamp": "2025-11-12T10:30:00",
  "errors": {
    "email": "Email is required",
    "password": "Password must be at least 8 characters"
  }
}
```

---

### Example 2: User Registration with Duplicate Email

```
POST /api/auth/register with existing email
       ↓
AuthService.register() validates email uniqueness
       ↓
userRepository.existsByEmail() returns true
       ↓
Throw ConflictException("User with this email already exists")
       ↓
BusinessLogicExceptionHandler.handleConflictException()
       ↓
Returns 409 Conflict
```

**Response (409):**
```json
{
  "status": 409,
  "message": "User with this email already exists",
  "timestamp": "2025-11-12T10:30:00"
}
```

---

### Example 3: User Login with Wrong Password

```
POST /api/auth/login with wrong password
       ↓
AuthService.login() finds user
       ↓
Password validation fails
       ↓
Throw UnauthorizedException("Invalid credentials")
       ↓
AuthenticationExceptionHandler.handleUnauthorizedException()
       ↓
Returns 401 Unauthorized
```

**Response (401):**
```json
{
  "status": 401,
  "message": "Invalid credentials",
  "timestamp": "2025-11-12T10:30:00"
}
```

---

### Example 4: Resource Not Found

```
GET /api/users/999
       ↓
UserService.getUserById(999) called
       ↓
userRepository.findById(999) returns empty
       ↓
Throw ResourceNotFoundException.withId("User", 999)
       ↓
ResourceExceptionHandler.handleResourceNotFound()
       ↓
Returns 404 Not Found
```

**Response (404):**
```json
{
  "status": 404,
  "message": "User with ID 999 not found",
  "timestamp": "2025-11-12T10:30:00"
}
```

---

### Example 5: Database Operation Failure

```
DELETE /api/projects/1
       ↓
ProjectService.deleteProject(1) called
       ↓
projectRepository.deleteById(1) fails
       ↓
DataAccessException thrown
       ↓
Catch and wrap: new DatabaseException("Failed to delete project", ex)
       ↓
SystemExceptionHandler.handleDatabaseException()
       ↓
Returns 500 Internal Server Error (details logged)
```

**Response (500):**
```json
{
  "status": 500,
  "message": "Database operation failed",
  "timestamp": "2025-11-12T10:30:00"
}
```

---

## Testing Exception Handlers

### Unit Testing Service Layer

```java
@ExtendWith(MockitoExtension.class)
public class AuthServiceExceptionTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private AuthService authService;
    
    @Test
    void register_whenEmailExists_throwsConflictException() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        
        // When & Then
        assertThrows(ConflictException.class, () -> {
            authService.register(request);
        });
    }
    
    @Test
    void register_withValidRequest_succeeds() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        
        // When & Then
        assertDoesNotThrow(() -> authService.register(request));
    }
    
    @Test
    void getUserById_whenNotFound_throwsResourceNotFoundException() {
        // Given
        when(userRepository.findById(999)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            authService.getUserProfile(999);
        });
    }
}
```

### Integration Testing Exception Handlers

```java
@WebMvcTest(AuthController.class)
public class ExceptionHandlerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private AuthService authService;
    
    @Test
    void register_whenConflictException_returns409() throws Exception {
        // Given
        when(authService.register(any())).thenThrow(
            new ConflictException("Email already exists")
        );
        
        // When & Then
        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"test@example.com\",\"password\":\"pass123\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value("Email already exists"));
    }
    
    @Test
    void getProfile_whenNotFound_returns404() throws Exception {
        // Given
        when(authService.getUserProfile(999)).thenThrow(
            ResourceNotFoundException.withId("User", 999)
        );
        
        // When & Then
        mockMvc.perform(get("/api/auth/profile?userId=999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").exists());
    }
    
    @Test
    void login_withInvalidCredentials_returns401() throws Exception {
        // Given
        when(authService.login(any())).thenThrow(
            new UnauthorizedException("Invalid credentials")
        );
        
        // When & Then
        mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"test@example.com\",\"password\":\"wrong\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
    }
}
```

---

## Extension Points

### Adding a New Exception

1. **Create the exception class** in `auth/util/exception/classes/`:
   ```java
   public class RateLimitExceededException extends ClientErrorException {
       public RateLimitExceededException(String message) {
           super(HttpStatus.TOO_MANY_REQUESTS.value(), message);
       }
       
       public RateLimitExceededException(String message, Throwable cause) {
           super(HttpStatus.TOO_MANY_REQUESTS.value(), message, cause);
       }
   }
   ```

2. **Create a handler** in `auth/util/exception/handlers/`:
   ```java
   @RestControllerAdvice
   public class RateLimitExceptionHandler {
       
       @ExceptionHandler(RateLimitExceededException.class)
       @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
       public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException ex) {
           ErrorResponse response = new ErrorResponse(
               HttpStatus.TOO_MANY_REQUESTS.value(),
               ex.getMessage()
           );
           return new ResponseEntity<>(response, HttpStatus.TOO_MANY_REQUESTS);
       }
   }
   ```

3. **Register in GlobalExceptionHandler:**
   ```java
   @ControllerAdvice(
       basePackageClasses = {
           ValidationExceptionHandler.class,
           ResourceExceptionHandler.class,
           AuthenticationExceptionHandler.class,
           BusinessLogicExceptionHandler.class,
           SystemExceptionHandler.class,
           RateLimitExceptionHandler.class  // Add here
       }
   )
   public class GlobalExceptionHandler {
   }
   ```

4. **Determine the exception hierarchy:**
   - If it's a 4xx error: Extend `ClientErrorException`
   - If it's a 5xx error: Extend `ServerErrorException`
   - For custom status codes: Extend `ApplicationException` directly

### Pattern for New Exceptions

**Simple Exception (inherits from category):**
```java
public class CustomException extends ClientErrorException {
    public CustomException(String message) {
        super(HttpStatus.CUSTOM.value(), message);
    }
}
```

**Exception with Code/Category:**
```java
public class SpecialException extends ServerErrorException {
    private String code;
    
    public SpecialException(String message, String code) {
        super(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
}
```

---

## Common Scenarios and Exception Mapping

| Scenario | Exception | HTTP Status | Example Message |
|---|---|---|---|
| Missing required field in POST body | `BadRequestException` | 400 | "Email is required" |
| Invalid email format | `BadRequestException` | 400 | "Invalid email format" |
| Invalid authentication token | `UnauthorizedException` | 401 | "Invalid or expired token" |
| Wrong password | `UnauthorizedException` | 401 | "Invalid credentials" |
| User lacks permission | `ForbiddenException` | 403 | "You do not have permission to delete this resource" |
| Resource doesn't exist | `ResourceNotFoundException` | 404 | "User not found with ID: 999" |
| Duplicate email/username | `ConflictException` | 409 | "Email already registered" |
| Invalid state transition | `ConflictException` | 409 | "Cannot revert completed project" |
| Restricted keywords in input | `UnprocessableEntityException` | 422 | "Project name contains restricted keywords" |
| Database connection fails | `DatabaseException` | 500 | "Database operation failed" |
| Unexpected server error | `Exception` (generic) | 500 | "An unexpected error occurred" |

---

## Summary

The CrudCloud exception handling system provides:

- **Clarity:** Developers know which exception to throw and when
- **Consistency:** All exceptions follow the same hierarchy and patterns
- **Maintainability:** Organized structure with clear separation of concerns
- **Scalability:** New exceptions can be added without disrupting existing code
- **User Experience:** Clients receive meaningful, consistent error responses
- **REST Compliance:** Proper HTTP status codes and response formats
- **Type Safety:** Category-based exception catching (4xx vs 5xx)
- **Debugging:** Built-in HTTP status codes with optional field-level error details

By following this architecture, the application ensures robust error handling that is easy to understand, maintain, and extend.

---

**Last Updated:** November 13, 2025  
**Version:** 3.0  
**Status:** ✅ Updated - Documentation now reflects three-tier exception hierarchy  
**Aligned With:** Exception implementation in `auth/util/exception/` with `ApplicationException`, `ClientErrorException`, and `ServerErrorException` base classes
