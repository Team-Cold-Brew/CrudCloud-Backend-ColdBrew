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
        ├── GlobalExceptionHandler.java
        ├── classes/
        │   ├── AuthException.java
        │   ├── BadRequestException.java
        │   ├── ConflictException.java
        │   ├── DatabaseException.java
        │   ├── ForbiddenException.java
        │   ├── InvalidCredentialsException.java (deprecated)
        │   ├── ResourceNotFoundException.java
        │   ├── UnauthorizedException.java
        │   ├── UnprocessableEntityException.java
        │   ├── UserAlreadyExistsException.java (deprecated)
        │   └── UserNotFoundException.java (deprecated)
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

## Custom Exceptions

### 1. **BadRequestException** (HTTP 400)

```java
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
```

**Purpose:** Thrown when a request contains syntactically invalid data.

**HTTP Status:** 400 Bad Request

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

---

### 2. **UnauthorizedException** (HTTP 401)

```java
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
```

**Purpose:** Thrown when authentication fails (invalid credentials).

**HTTP Status:** 401 Unauthorized

**Key Distinction:**
- **401 Unauthorized** = "I don't know who you are" (authentication failure)
- **403 Forbidden** = "I know who you are, but you can't do that" (authorization failure)

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

---

### 3. **ForbiddenException** (HTTP 403)

```java
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
```

**Purpose:** Thrown when a user lacks authorization to access a resource.

**HTTP Status:** 403 Forbidden

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

---

### 4. **ResourceNotFoundException** (HTTP 404)

```java
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    // Factory methods for common scenarios
    public static ResourceNotFoundException withResource(String resourceType, String identifier) {
        return new ResourceNotFoundException(
            String.format("%s with %s not found", resourceType, identifier)
        );
    }
    
    public static ResourceNotFoundException withId(String resourceType, Integer id) {
        return new ResourceNotFoundException(
            String.format("%s with ID %d not found", resourceType, id)
        );
    }
}
```

**Purpose:** Thrown when a requested resource cannot be found in the database.

**HTTP Status:** 404 Not Found

**When to Use:**
- Project/Plan ID doesn't exist
- User requests a non-existent resource
- Database query returns no results

**Example:**
```java
return repository.findById(id)
    .orElseThrow(() -> ResourceNotFoundException.withId("Project", id));
```

---

### 5. **ConflictException** (HTTP 409)

```java
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
```

**Purpose:** Thrown when a request conflicts with the current state of the resource or violates uniqueness constraints.

**HTTP Status:** 409 Conflict

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

---

### 6. **UnprocessableEntityException** (HTTP 422)

```java
public class UnprocessableEntityException extends RuntimeException {
    public UnprocessableEntityException(String message) {
        super(message);
    }
}
```

**Purpose:** Thrown when the request is syntactically valid but semantically incorrect.

**HTTP Status:** 422 Unprocessable Entity

**Key Distinction from BadRequestException:**
- **400 (BadRequest):** Syntax is invalid (JSON doesn't parse, missing required fields)
- **422 (UnprocessableEntity):** Syntax is valid, but violates business rules

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

---

### 7. **DatabaseException** (HTTP 500)

```java
public class DatabaseException extends RuntimeException {
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**Purpose:** Thrown when database operations fail unexpectedly.

**HTTP Status:** 500 Internal Server Error

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

---

## Exception Handlers

### Handler Pattern

Each handler class is responsible for a specific category of exceptions and their corresponding HTTP status codes. Handlers implement the following pattern:

```java
@ExceptionHandler(CustomException.class)
@ResponseStatus(HttpStatus.XXX)
public ResponseEntity<ErrorResponse> handleException(CustomException ex) {
    ErrorResponse response = new ErrorResponse(
        HttpStatus.XXX.value(),
        ex.getMessage(),
        LocalDateTime.now()
    );
    return new ResponseEntity<>(response, HttpStatus.XXX);
}
```

### 1. **ValidationExceptionHandler** (HTTP 400)

**Handles:**
- `BadRequestException` → HTTP 400
- `MethodArgumentNotValidException` → HTTP 400

**Responsibilities:**
- Validates request syntax
- Handles Spring's built-in validation failures
- Provides detailed field-level error messages

**Response Format for MethodArgumentNotValidException:**
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

### 2. **AuthenticationExceptionHandler** (HTTP 401 & 403)

**Handles:**
- `UnauthorizedException` → HTTP 401
- `ForbiddenException` → HTTP 403

**Responsibilities:**
- Authentication failure detection
- Authorization checks
- Permission validation
- Access control enforcement

**Response Format (401):**
```json
{
  "status": 401,
  "message": "Invalid credentials",
  "timestamp": "2025-11-12T10:30:00"
}
```

**Response Format (403):**
```json
{
  "status": 403,
  "message": "You do not have permission to access this resource",
  "timestamp": "2025-11-12T10:30:00"
}
```

---

### 3. **ResourceExceptionHandler** (HTTP 404)

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
  "message": "User with ID 999 not found",
  "timestamp": "2025-11-12T10:30:00"
}
```

---

### 4. **BusinessLogicExceptionHandler** (HTTP 409 & 422)

**Handles:**
- `ConflictException` → HTTP 409 Conflict
- `UnprocessableEntityException` → HTTP 422 Unprocessable Entity

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
  "timestamp": "2025-11-12T10:30:00"
}
```

**Response Format (422):**
```json
{
  "status": 422,
  "message": "Project name contains restricted keywords",
  "timestamp": "2025-11-12T10:30:00"
}
```

---

### 5. **SystemExceptionHandler** (HTTP 500)

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
  "timestamp": "2025-11-12T10:30:00"
}
```

---

### GlobalExceptionHandler (Orchestrator)

Located in `auth/util/exception/GlobalExceptionHandler.java`:

```java
@ControllerAdvice(basePackageClasses = {
    ValidationExceptionHandler.class,
    AuthenticationExceptionHandler.class,
    ResourceExceptionHandler.class,
    BusinessLogicExceptionHandler.class,
    SystemExceptionHandler.class
})
public class GlobalExceptionHandler {
    // Orchestrator - delegates to specialized handlers
}
```

**Role:** 
- Acts as the central registration point for all exception handlers
- Uses `@ControllerAdvice` to register the application with Spring
- Delegates specific exception types to appropriate handler classes

**Benefits of This Architecture:**
- ✅ **Separation of Concerns:** Each handler manages one category of exceptions
- ✅ **Maintainability:** Easy to locate and modify specific exception handling
- ✅ **Scalability:** New handlers can be added without modifying existing code
- ✅ **Single Responsibility:** Each handler focuses on one task
- ✅ **Reusability:** Handler patterns can be copied for new exceptions

---

### Error Response DTO

Located in `auth/dto/ErrorResponse.java`:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private int status;                       // HTTP status code
    private String message;                   // Human-readable message
    private LocalDateTime timestamp;          // When error occurred
    private Map<String, String> errors;       // Optional field-level errors
    private String path;                      // Optional request path
}
```

**Fields:**
- `status` - HTTP status code (400, 401, 403, 404, 409, 422, 500)
- `message` - Clear, user-friendly error message
- `timestamp` - ISO 8601 formatted timestamp
- `errors` - Optional map of field-level validation errors
- `path` - Optional request path that caused the error

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

1. **Create the exception class** in `auth/exception/`:
   ```java
   public class RateLimitExceededException extends RuntimeException {
       public RateLimitExceededException(String message) {
           super(message);
       }
   }
   ```

2. **Create a handler** in `auth/exception/handlers/`:
   ```java
   @RestControllerAdvice
   public class RateLimitExceptionHandler {
       
       @ExceptionHandler(RateLimitExceededException.class)
       @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
       public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException ex) {
           ErrorResponse response = new ErrorResponse(
               429,
               ex.getMessage(),
               LocalDateTime.now()
           );
           return new ResponseEntity<>(response, HttpStatus.TOO_MANY_REQUESTS);
       }
   }
   ```

3. **Register in GlobalExceptionHandler:**
   ```java
   @ControllerAdvice(basePackageClasses = {
       ValidationExceptionHandler.class,
       AuthenticationExceptionHandler.class,
       ResourceExceptionHandler.class,
       BusinessLogicExceptionHandler.class,
       SystemExceptionHandler.class,
       RateLimitExceptionHandler.class  // Add here
   })
   public class GlobalExceptionHandler {
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
| Resource doesn't exist | `ResourceNotFoundException` | 404 | "User with ID 999 not found" |
| Duplicate email/username | `ConflictException` | 409 | "Email already registered" |
| Invalid state transition | `ConflictException` | 409 | "Cannot revert completed project" |
| Restricted keywords in input | `UnprocessableEntityException` | 422 | "Project name contains restricted keywords" |
| Database connection fails | `DatabaseException` | 500 | "Database operation failed" |
| Unexpected server error | `Exception` (generic) | 500 | "An unexpected error occurred" |

---

## Summary

The CrudCloud exception handling system provides:

- **Clarity:** Developers know which exception to throw and when
- **Consistency:** All exceptions follow the same format and patterns
- **Maintainability:** Organized structure makes changes easy
- **Scalability:** New exceptions can be added without disrupting existing code
- **User Experience:** Clients receive meaningful, consistent error responses
- **REST Compliance:** Proper HTTP status codes and response formats
- **Debugging:** Detailed error messages with optional field-level context

By following this architecture, the application ensures robust error handling that is easy to understand, maintain, and extend.

---

**Last Updated:** November 12, 2025  
**Version:** 2.1  
**Aligned With:** EXCEPTION_HANDLING_IMPLEMENTATION.md  
**Status:** Refactored - Exception package moved to `auth/util/exception/` with organized subdirectories
