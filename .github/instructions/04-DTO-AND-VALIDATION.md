# 📮 DTO & Validation Strategy

## Request DTOs (Input Validation)

### Using Bean Validation Annotations

Request DTOs should validate input at the controller boundary:

```java
// ✅ Use Bean Validation annotations
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateInstanceRequest {
    
    @NotNull(message = "Engine is required")
    @Pattern(regexp = "MYSQL|POSTGRESQL|MONGODB|REDIS|CASSANDRA|SQLSERVER")
    private String engine;
    
    @NotBlank(message = "Instance name is required")
    @Length(min = 3, max = 100)
    private String name;
    
    @Min(0)
    private Integer organizationId;
    
    @Email(message = "Must be valid email")
    private String notificationEmail;
    
    @Positive(message = "Memory must be positive")
    private Integer memoryMb;
}
```

### Triggering Validation in Controllers

```java
// ✅ Validation triggered automatically by Spring
@RestController
@RequestMapping("/api/v1/instances")
public class InstanceController {
    
    @PostMapping
    public ResponseEntity<InstanceResponse> createInstance(
        @Valid @RequestBody CreateInstanceRequest req  // @Valid triggers validation
    ) {
        // If validation fails, GlobalExceptionHandler catches MethodArgumentNotValidException
        Instance instance = instanceService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(instance));
    }
}
```

### Common Validation Annotations

| Annotation | Purpose | Example |
|-----------|---------|---------|
| `@NotNull` | Field cannot be null | `@NotNull private String name;` |
| `@NotBlank` | String cannot be empty/whitespace | `@NotBlank private String email;` |
| `@NotEmpty` | Collection cannot be empty | `@NotEmpty private List<String> tags;` |
| `@Email` | Must be valid email format | `@Email private String contact;` |
| `@Pattern` | Must match regex | `@Pattern(regexp = "^[A-Z]+$")` |
| `@Length` | String length bounds | `@Length(min=3, max=100)` |
| `@Min` / `@Max` | Numeric bounds | `@Min(1) @Max(100) private int count;` |
| `@Positive` / `@Negative` | Must be positive/negative | `@Positive private int port;` |
| `@Future` / `@Past` | Date constraints | `@Future private LocalDateTime expiresAt;` |

---

## Response DTOs (Data Transfer)

### Design for API Consumption

Response DTOs should expose only what clients need, never internal details:

```java
// ✅ Response DTO exposes only necessary data
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstanceResponse {
    private UUID instanceId;
    private String engine;
    private String name;
    private String host;
    private Integer port;
    private String username;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // ❌ NEVER include:
    // private String password;           // Sensitive data
    // private String internalDockerId;   // Implementation detail
    // private byte[] rawConfig;          // Internal representation
}
```

### Conversion from Entity to DTO

```java
@Service
public class InstanceService {
    
    public InstanceResponse toResponse(Instance instance) {
        return new InstanceResponse(
            instance.getId(),
            instance.getEngine(),
            instance.getName(),
            instance.getHost(),
            instance.getPort(),
            instance.getUsername(),
            instance.getStatus().toString(),
            instance.getCreatedAt(),
            instance.getUpdatedAt()
        );
    }
    
    // Or use mapper (MapStruct recommended)
    private InstanceMapper mapper;
    
    public InstanceResponse toResponse(Instance instance) {
        return mapper.toResponse(instance);
    }
}
```

### Response DTOs for Collections

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstanceListResponse {
    private List<InstanceResponse> instances;
    private int total;
    private int page;
    private int pageSize;
}

// Usage in controller
@GetMapping
public ResponseEntity<InstanceListResponse> listInstances(
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "20") int pageSize
) {
    Page<Instance> instances = instanceService.list(page - 1, pageSize);
    InstanceListResponse response = new InstanceListResponse(
        instances.getContent().stream()
            .map(this::toResponse)
            .collect(Collectors.toList()),
        (int) instances.getTotalElements(),
        page,
        pageSize
    );
    return ResponseEntity.ok(response);
}
```

---

## Special Cases: Creation Responses

### When to Include Sensitive Data (One-Time Only)

For operations like creating database instances, show sensitive data only on creation:

```java
// ✅ Include credentials ONLY on creation
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstanceCreatedResponse {
    private UUID instanceId;
    private String engine;
    private String name;
    private String host;
    private Integer port;
    private String username;
    private String password;  // Shown ONLY here, on creation
    private String status;
    private LocalDateTime createdAt;
    
    public InstanceCreatedResponse(Instance instance, String plainPassword) {
        this.instanceId = instance.getId();
        this.engine = instance.getEngine();
        this.name = instance.getName();
        this.host = instance.getHost();
        this.port = instance.getPort();
        this.username = instance.getUsername();
        this.password = plainPassword;  // Only temporary
        this.status = instance.getStatus().toString();
        this.createdAt = instance.getCreatedAt();
    }
}

// Usage
@PostMapping
public ResponseEntity<InstanceCreatedResponse> createInstance(
    @Valid @RequestBody CreateInstanceRequest req
) {
    Instance instance = instanceService.create(req);
    String plainPassword = instance.getGeneratedPassword();  // Before hashing
    
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new InstanceCreatedResponse(instance, plainPassword));
}
```

---

## Error Response DTOs

### Handled by GlobalExceptionHandler

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String code;                    // Machine-readable error code
    private String message;                 // Human-readable message
    private Map<String, Object> details;    // Additional context
    private Instant timestamp;              // When error occurred
}

// Example response:
{
    "code": "PLAN_LIMIT_EXCEEDED",
    "message": "Plan limit of 2 instances exceeded",
    "details": {
        "currentPlan": "FREE",
        "maxInstances": 2
    },
    "timestamp": "2025-11-20T14:30:45.123Z"
}
```

---

## Best Practices

### ✅ DO: Validate at Boundary

```java
// ✅ Validate in controller using @Valid
@PostMapping
public ResponseEntity<InstanceResponse> createInstance(
    @Valid @RequestBody CreateInstanceRequest req
) {
    // Request is guaranteed valid here
}
```

### ✅ DO: Group Validation (for complex scenarios)

```java
@Data
public class PlanUpgradeRequest {
    @NotNull(groups = BasicValidation.class)
    private String planId;
    
    @NotNull(groups = PaymentValidation.class)
    private String paymentMethodId;
    
    @NotNull(groups = PaymentValidation.class)
    private String billingAddress;
}

@PostMapping
public ResponseEntity<?> upgradePlan(
    @Valid(groups = {BasicValidation.class, PaymentValidation.class})
    @RequestBody PlanUpgradeRequest req
) {
    // Both groups validated
}
```

### ✅ DO: Use Custom Validators for Complex Logic

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = InstanceConfigValidator.class)
public @interface ValidInstanceConfig {
    String message() default "Invalid instance configuration";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class InstanceConfigValidator implements ConstraintValidator<ValidInstanceConfig, CreateInstanceRequest> {
    @Override
    public boolean isValid(CreateInstanceRequest req, ConstraintValidatorContext ctx) {
        // Complex validation logic
        if ("MYSQL".equals(req.getEngine()) && req.getMemoryMb() < 256) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate(
                "MySQL requires at least 256MB memory"
            ).addConstraintViolation();
            return false;
        }
        return true;
    }
}

// Usage
@Data
@ValidInstanceConfig
public class CreateInstanceRequest {
    // Fields...
}
```

### ❌ DON'T: Validate in Service Layer (primary validation)

```java
// ❌ BAD: Validating in service
@Service
public class InstanceService {
    public Instance create(CreateInstanceRequest req) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw new IllegalArgumentException("Name required");
        }
    }
}

// ✅ GOOD: Validation in DTO, service assumes valid input
@Service
public class InstanceService {
    public Instance create(CreateInstanceRequest req) {
        // No validation needed - controller validated
        return instanceRepository.save(buildInstance(req));
    }
}
```

### ❌ DON'T: Expose Internal Fields

```java
// ❌ BAD: Exposing internal Hibernate proxy
@Data
public class UserResponse {
    private User internalUser;  // Never expose entities directly
}

// ✅ GOOD: Map to DTO
@Data
public class UserResponse {
    private UUID userId;
    private String email;
    private String fullName;
}
```

---

## Testing DTO Validation

```java
@ExtendWith(MockitoExtension.class)
public class CreateInstanceRequestValidationTest {
    
    private Validator validator;
    
    @BeforeEach
    public void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    @Test
    void validate_withValidRequest_succeeds() {
        CreateInstanceRequest req = new CreateInstanceRequest(
            "MYSQL", "my-db", null, null, 512
        );
        
        Set<ConstraintViolation<CreateInstanceRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }
    
    @Test
    void validate_withBlankName_fails() {
        CreateInstanceRequest req = new CreateInstanceRequest(
            "MYSQL", "", null, null, 512
        );
        
        Set<ConstraintViolation<CreateInstanceRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("name")));
    }
    
    @Test
    void validate_withInvalidEngine_fails() {
        CreateInstanceRequest req = new CreateInstanceRequest(
            "INVALID_ENGINE", "my-db", null, null, 512
        );
        
        Set<ConstraintViolation<CreateInstanceRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
    }
}
```

---

**Last Updated:** November 2025  
**Version:** 1.0
