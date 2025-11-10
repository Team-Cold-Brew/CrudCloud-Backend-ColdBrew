# 🔄 Module Communication Patterns

## Event-Driven Communication (Async)

### Pattern: When Module A needs to notify Module B of something

**Step 1: Define event in `core/event/`**

```java
public class InstanceCreatedEvent extends ApplicationEvent {
    private final UUID instanceId;
    private final UUID userId;
    private final String engine;
    
    public InstanceCreatedEvent(Object source, UUID instanceId, UUID userId, String engine) {
        super(source);
        this.instanceId = instanceId;
        this.userId = userId;
        this.engine = engine;
    }
    // Getters...
}
```

**Step 2: Publish from Instance module**

```java
@Service
public class InstanceService {
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    public Instance createInstance(CreateInstanceRequest req, User user) {
        Instance instance = buildInstance(req, user);
        instanceRepository.save(instance);
        
        // Publish event for other modules to react
        eventPublisher.publishEvent(
            new InstanceCreatedEvent(this, instance.getId(), user.getId(), req.getEngine())
        );
        
        return instance;
    }
}
```

**Step 3: Listen in Auth/Payment modules**

```java
@Component
public class InstanceEventListener {
    
    @EventListener
    public void onInstanceCreated(InstanceCreatedEvent event) {
        // Payment module: log for billing
        // Auth module: update user activity
        // Notification module: send email
    }
}
```

### Benefits

- ✅ **Loose coupling:** Initiator doesn't know about listeners
- ✅ **Scalability:** Easy to add new listeners without modifying initiator
- ✅ **Asynchronous:** Non-blocking, improves response time
- ✅ **Eventual consistency:** Changes propagate across modules

### Use Cases

- User registration → Assign plan to user
- Instance created → Log for billing, send notification
- Payment approved → Update user's plan
- Instance deleted → Clean up related records

---

## Module API Contracts (Sync)

### Pattern: When Module A needs to query data from Module B

**Step 1: Define public API in `payment/api/`**

```java
public interface PaymentModuleApi {
    boolean canUserCreateInstance(UUID userId, int currentInstanceCount);
    Plan getUserCurrentPlan(UUID userId);
}
```

**Step 2: Implement in `payment/service/`**

```java
@Service
public class PaymentModuleApiImpl implements PaymentModuleApi {
    @Autowired
    private PlanService planService;
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Override
    public boolean canUserCreateInstance(UUID userId, int currentInstanceCount) {
        Plan plan = getUserCurrentPlan(userId);
        return currentInstanceCount < plan.getMaxInstances();
    }
    
    @Override
    public Plan getUserCurrentPlan(UUID userId) {
        return planService.getUserPlan(userId);
    }
}
```

**Step 3: Inject in Instance module (depends on interface, not implementation)**

```java
@Service
public class InstanceService {
    @Autowired
    private PaymentModuleApi paymentApi;  // Interface!
    
    public Instance createInstance(CreateInstanceRequest req, User user) {
        if (!paymentApi.canUserCreateInstance(user.getId(), user.getInstances().size())) {
            throw new PlanLimitExceededException("Instance limit reached");
        }
        // Continue with instance creation...
    }
}
```

### Benefits

- ✅ **Encapsulation:** Interface hides implementation details
- ✅ **Testability:** Easy to mock for unit tests
- ✅ **Loose coupling:** Depends on abstraction, not concrete implementation
- ✅ **Phase 2 ready:** Interface becomes HTTP client in microservices

### Use Cases

- Check plan limits before creating instance
- Get user's current plan for validation
- Query instance quotas
- Retrieve user information

---

## ❌ FORBIDDEN Pattern: Direct Service Imports

```java
// ❌ DO NOT DO THIS
@Service
public class InstanceService {
    @Autowired
    private PaymentService paymentService;  // Violates module boundary!
    
    public Instance createInstance(CreateInstanceRequest req) {
        paymentService.validatePlan(req.getUserId());  // Tight coupling!
    }
}

// ✅ USE MODULE API INSTEAD
@Service
public class InstanceService {
    @Autowired
    private PaymentModuleApi paymentApi;  // Interface!
}
```

### Why This Is Bad

- ❌ **Tight coupling:** Modules become interdependent
- ❌ **Hard to test:** Can't isolate module for testing
- ❌ **Hard to refactor:** Changes ripple across modules
- ❌ **Hard to migrate:** Phase 2 microservices becomes difficult

---

## Cross-Module Communication Workflows

### Example 1: User Registration → Plan Assignment

```
AUTH Module (Initiator)
    ↓
    └→ User registers → AuthService.register()
       ├→ Create User entity
       ├→ Save to database
       ├→ Publish UserRegisteredEvent
       └→ Return JWT token
         
┌─────────────────────────────────────────────┐
│ UserRegisteredEvent propagates               │
└─────────────────────────────────────────────┘
       ↓
    PLAN Module (Listener)
       ├→ PlanEventListener.onUserRegistered()
       ├→ Assign FREE plan to new user
       └→ Publish UserAssignedPlanEvent
       
┌─────────────────────────────────────────────┐
│ UserAssignedPlanEvent propagates             │
└─────────────────────────────────────────────┘
       ↓
    PAYMENT Module (Listener)
       └→ PaymentEventListener.onUserAssignedPlan()
          └→ Initialize user billing record
```

### Example 2: Create Instance (with Plan Validation)

```
INSTANCE Module (Initiator)
    ↓
    └→ createInstance(request)
       ├→ Get user from Auth via AuthModuleApi
       ├→ Check plan limits via PaymentModuleApi
       ├→ Validate limit not exceeded (throws if exceeded)
       ├→ Create Instance entity
       ├→ Save to repository
       ├→ Start Docker container (DockerOrchestrationService)
       ├→ Update instance status to RUNNING
       ├→ Publish InstanceCreatedEvent
       └→ Return InstanceResponse

┌─────────────────────────────────────────────┐
│ InstanceCreatedEvent propagates              │
└─────────────────────────────────────────────┘
       ├→ AUTH Module listens
       │  └→ Update user activity log
       │
       ├→ PAYMENT Module listens
       │  └→ Log event for billing/analytics
       │
       └→ NOTIFICATION Module listens
          └→ Send email to user with connection info
```

### Example 3: Upgrade Plan (Payment Processing)

```
PAYMENT Module (Initiator)
    ↓
    └→ processUpgrade(userId, newPlan)
       ├→ Create transaction with status=PENDING
       ├→ Create MercadoPago preference
       └→ Return checkout URL

User completes payment on MercadoPago
    ↓
MercadoPago sends webhook notification
    ↓
PAYMENT Module receives webhook
    ├→ PaymentController.handleWebhook()
    ├→ Verify signature
    ├→ Update transaction status=APPROVED
    ├→ Publish PaymentApprovedEvent
    └→ Return 200 OK to MercadoPago

┌─────────────────────────────────────────────┐
│ PaymentApprovedEvent propagates              │
└─────────────────────────────────────────────┘
       ├→ AUTH Module listens
       │  └→ Update user's plan
       │
       └→ PLAN Module listens
          └→ Update plan limits/features
```

---

## Summary: When to Use What?

| Scenario | Use | Why |
|----------|-----|-----|
| Module A needs to **notify** Module B of an event | **Events** | Async, loose coupling, scalable |
| Module A needs to **query** data from Module B | **Module API** | Sync, type-safe, testable |
| Module A needs **immediate result** from Module B | **Module API** | No waiting for async listeners |
| Module A only cares that something **happened** | **Events** | No need for response |
| Circular dependency risk | **Events** | Breaks cycles, events are one-way |

---

**Last Updated:** November 2025  
**Version:** 1.0
