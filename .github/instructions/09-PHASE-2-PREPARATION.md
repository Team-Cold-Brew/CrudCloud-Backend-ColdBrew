# 🚀 Phase 2: Microservices Preparation

When implementing features in Phase 1, keep these Phase 2 implications in mind. This is critical for ensuring smooth migration to microservices architecture.

---

## 1. Module APIs → HTTP Clients (Feign)

### Phase 1: In-Process Interface

```java
// In phase1/instance/api/InstanceModuleApi.java
public interface InstanceModuleApi {
    Instance createInstance(CreateInstanceRequest request);
    Instance getInstanceById(UUID id);
    List<Instance> getUserInstances(UUID userId);
    void deleteInstance(UUID id);
}

// In phase1/instance/service/InstanceModuleApiImpl.java
@Service
public class InstanceModuleApiImpl implements InstanceModuleApi {
    @Autowired
    private InstanceService instanceService;
    
    @Override
    public Instance createInstance(CreateInstanceRequest request) {
        return instanceService.create(request);
    }
    
    @Override
    public Instance getInstanceById(UUID id) {
        return instanceService.getById(id);
    }
}

// Usage in phase1/payment/service/
@Service
public class PaymentService {
    @Autowired
    private InstanceModuleApi instanceApi;  // Interface!
    
    public void handlePaymentApproved(UUID userId) {
        List<Instance> instances = instanceApi.getUserInstances(userId);
    }
}
```

### Phase 2: Microservice HTTP Client (Feign)

```java
// In phase2/instance-service-client/InstanceServiceClient.java
@FeignClient(name = "instance-service", url = "${instance-service.url}")
public interface InstanceServiceClient extends InstanceModuleApi {
    
    @PostMapping("/api/v1/instances")
    @Override
    Instance createInstance(@RequestBody CreateInstanceRequest request);
    
    @GetMapping("/api/v1/instances/{id}")
    @Override
    Instance getInstanceById(@PathVariable UUID id);
    
    @GetMapping("/api/v1/instances/user/{userId}")
    @Override
    List<Instance> getUserInstances(@PathVariable UUID userId);
    
    @DeleteMapping("/api/v1/instances/{id}")
    @Override
    void deleteInstance(@PathVariable UUID id);
}

// Configuration
@Configuration
public class FeignConfig {
    @Bean
    public ErrorDecoder errorDecoder() {
        return new CustomErrorDecoder();  // Convert HTTP errors to exceptions
    }
}

// Usage in phase2/payment-service/ - EXACTLY THE SAME
@Service
public class PaymentService {
    @Autowired
    private InstanceModuleApi instanceApi;  // Same interface!
    
    public void handlePaymentApproved(UUID userId) {
        List<Instance> instances = instanceApi.getUserInstances(userId);
    }
}
```

**Key Point:** The calling code doesn't change - only the implementation switches from direct call to HTTP call via Feign.

---

## 2. Events → Message Queue (Kafka/RabbitMQ)

### Phase 1: Spring Events (In-Memory)

```java
// phase1/core/event/
public class InstanceCreatedEvent extends ApplicationEvent {
    private final UUID instanceId;
    private final UUID userId;
    
    public InstanceCreatedEvent(Object source, UUID instanceId, UUID userId) {
        super(source);
        this.instanceId = instanceId;
        this.userId = userId;
    }
    // Getters...
}

// phase1/instance/service/
@Service
public class InstanceService {
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    public Instance createInstance(CreateInstanceRequest req, User user) {
        Instance instance = buildInstance(req, user);
        instanceRepository.save(instance);
        
        // Synchronous event (in-memory)
        eventPublisher.publishEvent(
            new InstanceCreatedEvent(this, instance.getId(), user.getId())
        );
        
        return instance;
    }
}

// phase1/payment/listener/
@Component
public class InstanceEventListener {
    
    @EventListener
    public void onInstanceCreated(InstanceCreatedEvent event) {
        // Payment module updates billing
        logger.info("Instance created, logging for billing: {}", event.getInstanceId());
    }
}
```

### Phase 2: Kafka Topics

```java
// phase2/instance-service/
@Service
public class InstanceService {
    @Autowired
    private KafkaTemplate<String, InstanceCreatedEvent> kafkaTemplate;
    
    public Instance createInstance(CreateInstanceRequest req, User user) {
        Instance instance = buildInstance(req, user);
        instanceRepository.save(instance);
        
        // Asynchronous event via Kafka
        kafkaTemplate.send("instance.created", 
            new InstanceCreatedEvent(this, instance.getId(), user.getId())
        );
        
        return instance;
    }
}

// Kafka configuration
@Configuration
public class KafkaConfig {
    
    @Bean
    public NewTopic instanceCreatedTopic() {
        return TopicBuilder.name("instance.created")
            .partitions(3)
            .replicas(1)
            .build();
    }
}

// phase2/payment-service/
@Service
public class PaymentKafkaListener {
    
    @KafkaListener(topics = "instance.created", groupId = "payment-service")
    public void onInstanceCreated(InstanceCreatedEvent event) {
        // Payment module listens to Kafka topic
        logger.info("Instance created, logging for billing: {}", event.getInstanceId());
    }
}
```

**Key Point:** Event structure remains the same, only transport mechanism changes (in-memory → Kafka).

---

## 3. Design for Eventual Consistency

### Phase 1: Anti-Pattern (Avoid This)

```java
// ❌ WRONG: Immediate query after event triggers
@Service
public class PaymentService {
    @Autowired
    private InstanceModuleApi instanceApi;
    
    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        // Immediately query Instance module
        List<Instance> instances = instanceApi.getUserInstances(event.getUserId());
        // May be empty because Instance module hasn't processed yet!
    }
}
```

### Phase 1: Correct Pattern

```java
// ✅ CORRECT: Separate concerns, eventual consistency
@Service
public class PaymentService {
    
    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        // Payment module only does its own thing
        User user = event.getUser();
        initializeBillingRecord(user);
        
        // Publish event for next step
        eventPublisher.publishEvent(
            new UserBillingInitializedEvent(this, user.getId())
        );
    }
}

// Instance module listens independently
@Service
public class InstanceService {
    
    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        // Initialize instance settings for new user
        userInstanceSettingsRepository.save(
            new UserInstanceSettings(event.getUserId())
        );
    }
}
```

### Phase 2: Same Pattern, Different Transport

```java
// phase2/payment-service/ - Structure unchanged
@Service
public class PaymentService {
    
    @KafkaListener(topics = "user.registered", groupId = "payment-service")
    public void onUserRegistered(UserRegisteredEvent event) {
        // Same logic, Kafka delivery instead of in-memory
        User user = event.getUser();
        initializeBillingRecord(user);
        
        kafkaTemplate.send("user.billing.initialized", 
            new UserBillingInitializedEvent(this, user.getId())
        );
    }
}
```

---

## 4. Idempotency Considerations

### Webhook Processing - Design for Idempotency

```java
// ✅ CORRECT: Design idempotent webhook handlers
@Service
public class PaymentService {
    
    public void processWebhook(WebhookPayload payload) {
        // Check if already processed
        Transaction existing = transactionRepository
            .findByProviderTransactionId(payload.getTransactionId());
        
        if (existing != null && existing.isApproved()) {
            // Already processed - return safely (idempotent)
            logger.info("Transaction already processed: {}", payload.getTransactionId());
            return;
        }
        
        // Process for first time
        Transaction transaction = createTransaction(payload);
        transactionRepository.save(transaction);
        
        eventPublisher.publishEvent(
            new PaymentApprovedEvent(this, transaction)
        );
    }
}

// ❌ WRONG: Non-idempotent processing
public void processWebhook(WebhookPayload payload) {
    // No check for duplicates
    Transaction transaction = createTransaction(payload);
    transactionRepository.save(transaction);  // Could save duplicate!
}
```

### In Phase 2: Same Pattern Works

```java
// phase2/payment-service/ - NO CHANGES NEEDED
@KafkaListener(topics = "webhook.payment", groupId = "payment-service")
public void processWebhook(WebhookPayload payload) {
    // Same idempotent logic works with Kafka retries
    Transaction existing = transactionRepository
        .findByProviderTransactionId(payload.getTransactionId());
    
    if (existing != null && existing.isApproved()) {
        return;  // Handles Kafka redelivery automatically
    }
    
    Transaction transaction = createTransaction(payload);
    transactionRepository.save(transaction);
}
```

---

## 5. Transaction Boundaries

### Document Transaction Boundaries

```java
// ✅ CORRECT: Document transaction scope
@Service
public class InstanceService {
    
    /**
     * Creates an instance with the following transaction boundaries:
     * 
     * TX1: Create Instance record + update user instance count (local transaction)
     * EVENT: InstanceCreatedEvent published (eventually consistent)
     * EVENT: PaymentService listens and updates billing (separate transaction)
     * EVENT: NotificationService listens and sends email (separate transaction)
     * 
     * Failure scenarios:
     * - TX1 fails: Instance not created, no event published
     * - Event publishing fails: Retry mechanism via Spring Events/Kafka
     * - PaymentService fails: Retries independently, doesn't affect Instance
     * 
     * In Phase 2, this becomes multiple microservice calls with Saga pattern
     */
    @Transactional
    public Instance createInstance(CreateInstanceRequest request, User user) {
        // TX1 logic
        Instance instance = new Instance();
        instance.setName(request.getName());
        instance.setEngine(request.getEngine());
        instance.setUser(user);
        instanceRepository.save(instance);
        
        user.setInstanceCount(user.getInstanceCount() + 1);
        userRepository.save(user);
        
        // EVENT - outside transaction
        eventPublisher.publishEvent(
            new InstanceCreatedEvent(this, instance.getId(), user.getId())
        );
        
        return instance;
    }
}
```

### Phase 2: Saga Pattern

```java
// phase2: Replaces @Transactional with Saga orchestration
@Service
public class CreateInstanceSaga {
    
    // Initiator: Instance service
    public void execute(CreateInstanceRequest request, UUID userId) {
        // Step 1: Create instance
        InstanceCreatedEvent event = createInstanceStep(request, userId);
        
        // Step 2: Update payment/billing (Saga step)
        updateBillingStep(event);
        
        // Step 3: Send notification (Saga step)
        sendNotificationStep(event);
        
        // Compensation on failure: compensateSteps()
    }
}
```

---

## 6. Shared Domain Objects → DTOs

### Phase 1: Shared Entities

```java
// phase1: All modules reference same entity
@Entity
@Table(name = "plans")
@Data
public class Plan {
    @Id
    private UUID id;
    private String name;
    private Integer maxInstances;
    private BigDecimal monthlyPrice;
}

// phase1/payment/service/
@Service
public class PaymentService {
    public Plan getUserPlan(UUID userId) {
        // Returns actual entity
        return planRepository.findByUserId(userId);
    }
}

// phase1/instance/service/
@Service
public class InstanceService {
    @Autowired
    private PaymentModuleApi paymentApi;
    
    public void createInstance(CreateInstanceRequest req, User user) {
        Plan plan = paymentApi.getUserPlan(user.getId());  // Shared entity
        
        if (user.getInstanceCount() >= plan.getMaxInstances()) {
            throw new PlanLimitExceededException();
        }
    }
}
```

### Phase 2: DTOs Between Services

```java
// phase2/payment-service/: Keep entity internal
@Entity
@Table(name = "plans")
@Data
private class PlanEntity {
    @Id
    private UUID id;
    private String name;
    private Integer maxInstances;
    private BigDecimal monthlyPrice;
}

// Expose via DTO
@Data
public class PlanDTO {
    private UUID id;
    private String name;
    private Integer maxInstances;
    private BigDecimal monthlyPrice;
}

// HTTP endpoint returns DTO
@GetMapping("/api/v1/plans/{id}")
public PlanDTO getPlan(@PathVariable UUID id) {
    PlanEntity entity = planRepository.findById(id).orElseThrow();
    return modelMapper.map(entity, PlanDTO.class);
}

// phase2/instance-service/: Receives DTO via Feign
@FeignClient(name = "payment-service")
public interface PaymentServiceClient {
    
    @GetMapping("/api/v1/plans/user/{userId}")
    PlanDTO getUserPlan(@PathVariable UUID userId);
}

// Uses DTO, not entity
@Service
public class InstanceService {
    @Autowired
    private PaymentServiceClient paymentClient;
    
    public void createInstance(CreateInstanceRequest req, UUID userId) {
        PlanDTO plan = paymentClient.getUserPlan(userId);  // DTO
        
        if (userInstanceCount >= plan.getMaxInstances()) {
            throw new PlanLimitExceededException();
        }
    }
}
```

---

## 7. Data Storage Strategy

### Phase 1: Single Database

```yaml
# All modules use same database
spring.datasource.url=jdbc:postgresql://localhost:5432/crudcloud
spring.datasource.username=postgres
spring.datasource.password=password

# Schema is shared
CREATE SCHEMA instance;
CREATE SCHEMA payment;
CREATE SCHEMA auth;
```

### Phase 2: Separate Databases

```yaml
# phase2/payment-service/
payment.datasource.url=jdbc:postgresql://localhost:5432/payment_db
payment.datasource.username=payment_user

# phase2/instance-service/
instance.datasource.url=jdbc:postgresql://localhost:5433/instance_db
instance.datasource.username=instance_user

# phase2/auth-service/
auth.datasource.url=jdbc:postgresql://localhost:5434/auth_db
auth.datasource.username=auth_user
```

**Key Point:** In Phase 1, design queries to work within module boundaries. Avoid cross-module joins even though possible.

```java
// ✅ CORRECT: Only query own module's tables
@Repository
public interface InstanceRepository extends JpaRepository<Instance, UUID> {
    
    @Query("SELECT i FROM Instance i WHERE i.user.id = :userId AND i.deletedAt IS NULL")
    List<Instance> findByUserId(@Param("userId") UUID userId);
    
    // Query stays within instance schema - works in Phase 2
}

// ❌ WRONG: Cross-module join
@Query("""
    SELECT i FROM Instance i 
    JOIN Payment p ON i.user.id = p.user.id
    WHERE i.user.id = :userId
""")
List<Instance> findByUserIdWithPaymentInfo(@Param("userId") UUID userId);
// Won't work in Phase 2 when Payment is separate DB!
```

---

## Phase 2 Readiness Checklist

When implementing features, ensure:

- ✅ All cross-module dependencies use `*ModuleApi` interfaces
- ✅ No direct service-to-service imports
- ✅ Events published for cross-module notifications
- ✅ No direct entity sharing across modules
- ✅ Webhook handlers are idempotent
- ✅ Transaction boundaries documented
- ✅ No cross-module joins in queries
- ✅ Module APIs return DTOs, not entities
- ✅ Configuration externalized (not hardcoded)
- ✅ Each module tests its contracts (contract tests)
- ✅ Error responses can be serialized to JSON
- ✅ No circular dependencies between modules

---

## Migration Timeline (Estimated)

```
Phase 1: MVP (Weeks 1-8)
├── Weeks 1-4: Core features (Auth, Instance, Payment)
├── Weeks 5-6: Integration & testing
├── Weeks 7-8: Production readiness
└── Result: Monolith with clean module boundaries

Phase 2: Microservices (Weeks 9+)
├── Week 9: Infra setup (Kafka, service discovery)
├── Weeks 10-13: Extract services one-by-one
│   ├── Week 10: Auth Service
│   ├── Week 11: Payment Service
│   ├── Week 12: Instance Service
│   └── Week 13: Plan Service
├── Week 14: Cross-service integration
└── Result: Distributed microservices

Key: Minimal code changes due to Phase 1 preparation!
```

---

## Benefits of This Approach

- ✅ **Smooth Migration:** Extract services with confidence
- ✅ **Zero Business Logic Changes:** Only transport/infrastructure changes
- ✅ **Reduced Risk:** Module boundaries prevent coupling
- ✅ **Easy Testing:** Contract tests ensure compatibility
- ✅ **Team Parallelization:** Multiple teams can extract services simultaneously
- ✅ **Gradual Rollout:** Migrate services incrementally, not all at once

---

**Last Updated:** November 2025  
**Version:** 1.0 (Phase 1 - MVP)
