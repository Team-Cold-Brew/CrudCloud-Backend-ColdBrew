# ⚠️ Exception Handling Strategy

## Exception Hierarchy

### Base Exception Class

All custom exceptions must extend `ApiException`:

```java
// ✅ Define custom exceptions
public abstract class ApiException extends RuntimeException {
    private final String code;
    private final Map<String, Object> details;
    
    public ApiException(String code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = details;
    }
    
    public String getCode() { return code; }
    public Map<String, Object> getDetails() { return details; }
}
```

### Module-Specific Exceptions

Each module defines its own exceptions extending `ApiException`:

```java
// auth/exception/
public class AuthenticationException extends ApiException {
    public AuthenticationException(String message) {
        super("AUTH_ERROR", message, Map.of());
    }
}

public class UserAlreadyExistsException extends ApiException {
    public UserAlreadyExistsException(String email) {
        super(
            "USER_EXISTS",
            String.format("User with email %s already exists", email),
            Map.of("email", email)
        );
    }
}

// instance/exception/
public class InstanceCreationException extends ApiException {
    public InstanceCreationException(String message) {
        super("INSTANCE_CREATION_FAILED", message, Map.of());
    }
}

public class DockerOperationException extends ApiException {
    public DockerOperationException(String operation, Throwable cause) {
        super(
            "DOCKER_OPERATION_FAILED",
            String.format("Docker operation failed: %s", operation),
            Map.of("operation", operation, "cause", cause.getMessage())
        );
    }
}

// payment/exception/
public class PaymentProcessingException extends ApiException {
    public PaymentProcessingException(String message) {
        super("PAYMENT_FAILED", message, Map.of());
    }
}

public class InsufficientCreditsException extends ApiException {
    public InsufficientCreditsException(double required, double available) {
        super(
            "INSUFFICIENT_CREDITS",
            String.format("Insufficient credits: required %.2f, available %.2f", required, available),
            Map.of("required", required, "available", available)
        );
    }
}

// plan/exception/
public class PlanLimitExceededException extends ApiException {
    public PlanLimitExceededException(String currentPlan, int limit) {
        super(
            "PLAN_LIMIT_EXCEEDED",
            String.format("Plan limit of %d instances exceeded", limit),
            Map.of("currentPlan", currentPlan, "maxInstances", limit)
        );
    }
}
```

---

## Global Exception Handler

### Configuration

Located in `core/exception/GlobalExceptionHandler.java`:

```java
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        ErrorResponse response = new ErrorResponse(
            ex.getCode(),
            ex.getMessage(),
            ex.getDetails(),
            Instant.now()
        );
        HttpStatus status = determineStatus(ex);
        
        logger.warn("API Exception [{}]: {}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(status).body(response);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex) {
        Map<String, Object> details = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            details.put(error.getField(), error.getDefaultMessage())
        );
        
        ErrorResponse response = new ErrorResponse(
            "VALIDATION_ERROR",
            "Request validation failed",
            details,
            Instant.now()
        );
        
        logger.warn("Validation error: {}", details);
        return ResponseEntity.badRequest().body(response);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse response = new ErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            Map.of(),
            Instant.now()
        );
        
        logger.error("Unhandled exception", ex);
        return ResponseEntity.status(500).body(response);
    }
    
    private HttpStatus determineStatus(ApiException ex) {
        return switch(ex.getCode()) {
            case "AUTH_ERROR", "USER_NOT_FOUND" -> HttpStatus.UNAUTHORIZED;
            case "USER_EXISTS", "VALIDATION_ERROR" -> HttpStatus.BAD_REQUEST;
            case "PLAN_LIMIT_EXCEEDED", "INSUFFICIENT_CREDITS" -> HttpStatus.FORBIDDEN;
            case "INSTANCE_NOT_FOUND", "PAYMENT_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
```

### Error Response DTO

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String code;           // Machine-readable error code
    private String message;        // Human-readable message
    private Map<String, Object> details;  // Additional context
    private Instant timestamp;     // When error occurred
}
```

---

## Best Practices

### ✅ DO: Provide Context in Details Map

```java
public class InstanceCreationException extends ApiException {
    public InstanceCreationException(CreateInstanceRequest req, String reason) {
        super(
            "INSTANCE_CREATION_FAILED",
            "Failed to create instance: " + reason,
            Map.of(
                "engine", req.getEngine(),
                "name", req.getName(),
                "reason", reason
            )
        );
    }
}
```

### ✅ DO: Throw Early, Fail Fast

```java
@Service
public class InstanceService {
    
    public Instance createInstance(CreateInstanceRequest req, User user) {
        // Validate immediately
        if (!paymentApi.canUserCreateInstance(user.getId(), user.getInstances().size())) {
            throw new PlanLimitExceededException(
                user.getCurrentPlan().getName(),
                user.getCurrentPlan().getMaxInstances()
            );
        }
        
        // Only proceed if validation passes
        Instance instance = buildInstance(req, user);
        return instanceRepository.save(instance);
    }
}
```

### ✅ DO: Chain Exceptions for Debugging

```java
@Service
public class DockerService {
    
    public String startContainer(ContainerConfig config) {
        try {
            return dockerClient.startContainer(config);
        } catch (DockerException e) {
            throw new DockerOperationException("startContainer", e);
        }
    }
}
```

### ❌ DON'T: Catch and Ignore

```java
// ❌ BAD: Silently swallowing exceptions
try {
    instanceService.createInstance(req, user);
} catch (Exception e) {
    // Do nothing - bad!
}

// ✅ GOOD: Handle or re-throw
try {
    instanceService.createInstance(req, user);
} catch (PlanLimitExceededException e) {
    logger.warn("User reached plan limit", e);
    throw e;  // Let controller handle
}
```

### ❌ DON'T: Use Generic Exceptions

```java
// ❌ BAD: Too generic
throw new RuntimeException("Something went wrong");

// ✅ GOOD: Specific exception with context
throw new InstanceCreationException("Docker container failed to start: " + reason);
```

### ❌ DON'T: Expose Sensitive Data

```java
// ❌ BAD: Database password in error message
catch (SQLException e) {
    throw new ApiException("DB_ERROR", "Connection to jdbc:mysql://user:pass@host failed", ...);
}

// ✅ GOOD: Generic message, log details separately
catch (SQLException e) {
    logger.error("Database connection failed", e);
    throw new ApiException("DB_ERROR", "Database operation failed", Map.of());
}
```

---

## Exception Flow Examples

### Example 1: Validation Failure

```
User submits invalid CreateInstanceRequest
       ↓
Spring @Valid annotation detects violation
       ↓
MethodArgumentNotValidException thrown
       ↓
GlobalExceptionHandler.handleValidationException()
       ↓
Returns 400 Bad Request with field errors
```

### Example 2: Business Rule Violation

```
InstanceService.createInstance() called
       ↓
Check: paymentApi.canUserCreateInstance() → false
       ↓
Throw PlanLimitExceededException
       ↓
GlobalExceptionHandler.handleApiException()
       ↓
Returns 403 Forbidden with plan details
```

### Example 3: Infrastructure Failure

```
DockerService.startContainer() called
       ↓
Docker API call fails with DockerException
       ↓
Catch and wrap: new DockerOperationException(..., cause)
       ↓
Propagate up to controller
       ↓
GlobalExceptionHandler.handleApiException()
       ↓
Returns 500 Internal Server Error (logged)
```

---

## Testing Exception Handling

```java
@ExtendWith(MockitoExtension.class)
public class InstanceServiceExceptionTest {
    
    @Mock
    private PaymentModuleApi paymentApi;
    
    @InjectMocks
    private InstanceService service;
    
    @Test
    void createInstance_whenPlanLimitExceeded_throwsException() {
        // Given
        when(paymentApi.canUserCreateInstance(anyUUID(), eq(2))).thenReturn(false);
        
        // When & Then
        assertThrows(PlanLimitExceededException.class, () -> {
            service.createInstance(req, user);
        });
    }
    
    @Test
    void createInstance_exceptionHasCorrectDetails() {
        // When & Then
        PlanLimitExceededException ex = assertThrows(PlanLimitExceededException.class, () -> {
            service.createInstance(req, user);
        });
        
        assertEquals("PLAN_LIMIT_EXCEEDED", ex.getCode());
        assertEquals("FREE", ex.getDetails().get("currentPlan"));
    }
}
```

---

**Last Updated:** November 2025  
**Version:** 1.0
