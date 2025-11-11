# 🔌 REST API Endpoint Strategy

## Consistent Naming Convention

### Versioned API Paths

Always include version from the start for future compatibility:

```java
// ✅ Auth Module
@RestController
@RequestMapping("/api/v1/auth")  // Version included!
public class AuthController {
    @PostMapping("/register")       // POST /api/v1/auth/register
    @PostMapping("/login")          // POST /api/v1/auth/login
    @GetMapping("/profile")         // GET /api/v1/auth/profile
}

// ✅ Instance Module
@RestController
@RequestMapping("/api/v1/instances")
public class InstanceController {
    @PostMapping("")                // POST /api/v1/instances
    @GetMapping("")                 // GET /api/v1/instances
    @GetMapping("/{id}")            // GET /api/v1/instances/{id}
    @PutMapping("/{id}/suspend")    // PUT /api/v1/instances/{id}/suspend
    @PutMapping("/{id}/resume")     // PUT /api/v1/instances/{id}/resume
    @DeleteMapping("/{id}")         // DELETE /api/v1/instances/{id}
}

// ✅ Payment Module
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    @PostMapping("/create-preference")  // POST /api/v1/payments/create-preference
    @PostMapping("/webhook")            // POST /api/v1/payments/webhook
    @GetMapping("/history")             // GET /api/v1/payments/history
}

// ✅ Plan Module
@RestController
@RequestMapping("/api/v1/plans")
public class PlanController {
    @GetMapping("")                 // GET /api/v1/plans
    @GetMapping("/current")         // GET /api/v1/plans/current
}

// ✅ Catalog Module
@RestController
@RequestMapping("/api/v1/engines")
public class CatalogController {
    @GetMapping("")                 // GET /api/v1/engines
}
```

---

## HTTP Status Codes

### Use Correct Status Codes

```java
// 201 Created: Resource successfully created
@PostMapping
public ResponseEntity<InstanceResponse> createInstance(@Valid @RequestBody CreateInstanceRequest req) {
    Instance instance = instanceService.create(req);
    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(instance));
}

// 200 OK: Standard success response
@GetMapping("/{id}")
public ResponseEntity<InstanceResponse> getInstance(@PathVariable UUID id) {
    Instance instance = instanceService.getById(id);
    return ResponseEntity.ok(toResponse(instance));
}

// 200 OK: Operation succeeded
@PutMapping("/{id}/suspend")
public ResponseEntity<InstanceResponse> suspendInstance(@PathVariable UUID id) {
    Instance instance = instanceService.suspend(id);
    return ResponseEntity.ok(toResponse(instance));
}

// 204 No Content: Success with no response body
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteInstance(@PathVariable UUID id) {
    instanceService.delete(id);
    return ResponseEntity.noContent().build();
}

// 202 Accepted: Long-running operation accepted
@PostMapping("/{id}/restore")
public ResponseEntity<Void> restoreInstance(@PathVariable UUID id) {
    instanceService.scheduleRestore(id);  // Async operation
    return ResponseEntity.accepted().build();
}

// 400 Bad Request: Validation error (handled by @Valid)
@GetMapping
public ResponseEntity<?> listInstances(
    @Min(1) @RequestParam(defaultValue = "1") int page
) {
    // If page < 1, returns 400
}

// 401 Unauthorized: Authentication required
@GetMapping("/profile")
public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String token) {
    if (!tokenService.isValid(token)) {
        throw new AuthenticationException("Invalid token");
    }
}

// 403 Forbidden: Authenticated but not authorized
@PostMapping
public ResponseEntity<InstanceResponse> createInstance(
    @Valid @RequestBody CreateInstanceRequest req
) {
    if (!paymentApi.canUserCreateInstance(userId, count)) {
        throw new PlanLimitExceededException(...);  // 403
    }
}

// 404 Not Found: Resource doesn't exist
@GetMapping("/{id}")
public ResponseEntity<InstanceResponse> getInstance(@PathVariable UUID id) {
    Instance instance = instanceService.getById(id);  // Throws NotFoundException
    return ResponseEntity.ok(toResponse(instance));
}

// 409 Conflict: State conflict (e.g., duplicate key)
@PostMapping("/register")
public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
    try {
        User user = userService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    } catch (UserAlreadyExistsException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(...));
    }
}

// 500 Internal Server Error: Unhandled exception (logged)
// Automatically returned for unhandled exceptions
```

### Status Code Reference

| Code | Usage | Example |
|------|-------|---------|
| 200 | Standard success | `GET /api/v1/instances` |
| 201 | Resource created | `POST /api/v1/instances` |
| 202 | Accepted (async) | `POST /api/v1/instances/{id}/restore` |
| 204 | Success, no content | `DELETE /api/v1/instances/{id}` |
| 400 | Bad request/validation | Invalid input parameters |
| 401 | Unauthorized | Missing/invalid authentication |
| 403 | Forbidden | Insufficient permissions/plan limit |
| 404 | Not found | Resource doesn't exist |
| 409 | Conflict | User already exists, duplicate key |
| 500 | Internal error | Unhandled exception |

---

## RESTful Endpoint Design

### CRUD Operations

```java
@RestController
@RequestMapping("/api/v1/instances")
public class InstanceController {
    
    // CREATE: POST with request body
    @PostMapping
    public ResponseEntity<InstanceCreatedResponse> createInstance(
        @Valid @RequestBody CreateInstanceRequest req
    ) {
        Instance instance = instanceService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new InstanceCreatedResponse(instance, plainPassword));
    }
    
    // READ: GET with optional query parameters
    @GetMapping
    public ResponseEntity<InstanceListResponse> listInstances(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize,
        @RequestParam(required = false) String status
    ) {
        Page<Instance> instances = instanceService.list(page - 1, pageSize, status);
        return ResponseEntity.ok(toListResponse(instances, page, pageSize));
    }
    
    // READ ONE: GET with path variable
    @GetMapping("/{id}")
    public ResponseEntity<InstanceResponse> getInstance(@PathVariable UUID id) {
        Instance instance = instanceService.getById(id);
        return ResponseEntity.ok(toResponse(instance));
    }
    
    // UPDATE: PUT for complete replacement, PATCH for partial
    @PutMapping("/{id}")
    public ResponseEntity<InstanceResponse> updateInstance(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateInstanceRequest req
    ) {
        Instance instance = instanceService.update(id, req);
        return ResponseEntity.ok(toResponse(instance));
    }
    
    // DELETE: DELETE removes resource
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInstance(@PathVariable UUID id) {
        instanceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### Custom Actions (Non-CRUD)

For operations that don't map to standard CRUD, use action-based endpoints:

```java
@RestController
@RequestMapping("/api/v1/instances")
public class InstanceController {
    
    // ✅ Use verb + noun pattern for custom actions
    
    // Suspend a running instance
    @PutMapping("/{id}/suspend")
    public ResponseEntity<InstanceResponse> suspendInstance(@PathVariable UUID id) {
        Instance instance = instanceService.suspend(id);
        return ResponseEntity.ok(toResponse(instance));
    }
    
    // Resume a suspended instance
    @PutMapping("/{id}/resume")
    public ResponseEntity<InstanceResponse> resumeInstance(@PathVariable UUID id) {
        Instance instance = instanceService.resume(id);
        return ResponseEntity.ok(toResponse(instance));
    }
    
    // Restart an instance
    @PutMapping("/{id}/restart")
    public ResponseEntity<InstanceResponse> restartInstance(@PathVariable UUID id) {
        Instance instance = instanceService.restart(id);
        return ResponseEntity.ok(toResponse(instance));
    }
    
    // Get logs for an instance
    @GetMapping("/{id}/logs")
    public ResponseEntity<LogResponse> getInstanceLogs(
        @PathVariable UUID id,
        @RequestParam(defaultValue = "100") int lines
    ) {
        String logs = instanceService.getLogs(id, lines);
        return ResponseEntity.ok(new LogResponse(logs));
    }
    
    // Get statistics for an instance
    @GetMapping("/{id}/stats")
    public ResponseEntity<InstanceStatsResponse> getInstanceStats(@PathVariable UUID id) {
        InstanceStats stats = instanceService.getStats(id);
        return ResponseEntity.ok(new InstanceStatsResponse(stats));
    }
}
```

---

## Request Parameter Handling

### Path Parameters

```java
// For resource identification
@GetMapping("/{id}")
public ResponseEntity<InstanceResponse> getInstance(@PathVariable UUID id) {
    // id is required, part of URL
}

@GetMapping("/instances/{instanceId}/backups/{backupId}")
public ResponseEntity<BackupResponse> getBackup(
    @PathVariable UUID instanceId,
    @PathVariable UUID backupId
) {
    // Hierarchical resource structure
}
```

### Query Parameters

```java
// For filtering, pagination, sorting
@GetMapping
public ResponseEntity<InstanceListResponse> listInstances(
    @RequestParam(defaultValue = "1") @Min(1) int page,
    @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
    @RequestParam(required = false) String engine,
    @RequestParam(required = false) String status,
    @RequestParam(defaultValue = "created_at:DESC") String sort
) {
    // Query: GET /api/v1/instances?page=2&pageSize=10&engine=MYSQL&sort=name:ASC
}
```

### Request Body

```java
// For creating or updating resources
@PostMapping
public ResponseEntity<InstanceCreatedResponse> createInstance(
    @Valid @RequestBody CreateInstanceRequest req
) {
    // Body is required for POST/PUT/PATCH operations
}

@PutMapping("/{id}")
public ResponseEntity<InstanceResponse> updateInstance(
    @PathVariable UUID id,
    @Valid @RequestBody UpdateInstanceRequest req
) {
    // Both path variable and body are used
}
```

### Headers

```java
// For metadata, authentication, custom requirements
@GetMapping("/{id}")
public ResponseEntity<InstanceResponse> getInstance(
    @PathVariable UUID id,
    @RequestHeader(required = false, defaultValue = "application/json") String accept,
    @RequestHeader(required = false) String authorization
) {
    // Headers provide additional context
}
```

---

## Error Responses

### Consistent Error Format

All errors return standard format via GlobalExceptionHandler:

```json
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

### Common Error Scenarios

```java
// Missing required field
{
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "details": {
        "engine": "Engine is required",
        "name": "Instance name is required"
    }
}

// Resource not found
{
    "code": "INSTANCE_NOT_FOUND",
    "message": "Instance with ID abc123 not found",
    "details": {"instanceId": "abc123"}
}

// Authorization failure
{
    "code": "PLAN_LIMIT_EXCEEDED",
    "message": "Plan limit of 2 instances exceeded",
    "details": {"currentPlan": "FREE", "maxInstances": 2}
}
```

---

## Best Practices

### ✅ DO: Use Consistent Naming

- Use kebab-case for paths: `/create-preference`
- Use snake_case for query parameters: `?page_size=20`
- Use camelCase in JSON payloads: `{"memoryMb": 256}`

### ✅ DO: Version Your API

- Always include version in path: `/api/v1/`
- Plan for `/api/v2/` when breaking changes needed

### ✅ DO: Use Correct HTTP Methods

- GET: Retrieve data (safe, idempotent)
- POST: Create resource
- PUT: Replace entire resource (idempotent)
- PATCH: Partial update (not idempotent unless designed carefully)
- DELETE: Remove resource

### ❌ DON'T: Use Query Parameters for Resource ID

```java
// ❌ BAD: ID in query parameter
GET /api/v1/instances?id=abc123

// ✅ GOOD: ID in path
GET /api/v1/instances/abc123
```

### ❌ DON'T: Use POST for All Operations

```java
// ❌ BAD: GET operation as POST
POST /api/v1/instances/list

// ✅ GOOD: Use GET for retrieval
GET /api/v1/instances
```

---

**Last Updated:** November 2025  
**Version:** 1.0
