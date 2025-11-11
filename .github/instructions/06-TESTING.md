# 🧪 Testing Guidelines

## Unit Tests (No External Dependencies)

### Testing Pure Business Logic with Mocks

```java
// ✅ Test pure business logic
@ExtendWith(MockitoExtension.class)
public class PlanValidationServiceTest {
    
    @Mock
    private PlanRepository planRepository;
    
    @InjectMocks
    private PlanValidationService service;
    
    @Test
    void canCreateInstance_whenLimitNotReached_returnsTrue() {
        // Given
        Plan plan = new Plan("FREE", 2);
        when(planRepository.findById(1)).thenReturn(Optional.of(plan));
        
        // When
        boolean result = service.canCreateInstance(1, 1);  // 1 < 2
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void canCreateInstance_whenLimitReached_returnsFalse() {
        // Given
        Plan plan = new Plan("FREE", 2);
        when(planRepository.findById(1)).thenReturn(Optional.of(plan));
        
        // When
        boolean result = service.canCreateInstance(1, 2);  // 2 == 2
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void canCreateInstance_whenPlanNotFound_throwsException() {
        // Given
        when(planRepository.findById(999)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(PlanNotFoundException.class, () -> {
            service.canCreateInstance(999, 0);
        });
    }
}
```

### Coverage Target: 70%+

Aim for high coverage on business logic:

```bash
# Run tests with coverage report
mvn clean test jacoco:report

# Check target/site/jacoco/index.html for coverage metrics
# Target: 70%+ for business logic
# Target: 50%+ for utilities/helpers
```

---

## Integration Tests (Within Module)

### Testing Module-Level Integration

```java
// ✅ Test module integration with real Spring context
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Transactional  // Rollback after each test
public class InstanceControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private InstanceRepository instanceRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @BeforeEach
    public void setUp() {
        // Clean up before each test
        instanceRepository.deleteAll();
        userRepository.deleteAll();
    }
    
    @Test
    void createInstance_shouldSaveToDatabase() {
        // Given
        User user = userRepository.save(new User("user@example.com", "password"));
        CreateInstanceRequest request = new CreateInstanceRequest(
            "MYSQL", "my-db", null
        );
        
        // When
        ResponseEntity<InstanceResponse> response = restTemplate.postForEntity(
            "/api/v1/instances",
            request,
            InstanceResponse.class,
            Collections.singletonMap("Authorization", "Bearer " + generateToken(user))
        );
        
        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody().getInstanceId());
        assertEquals(1, instanceRepository.count());
    }
    
    @Test
    void getInstances_returnsOkWithData() {
        // Given
        User user = userRepository.save(new User("user@example.com", "password"));
        Instance instance = instanceRepository.save(
            new Instance("MYSQL", "db-1", user)
        );
        
        // When
        ResponseEntity<InstanceListResponse> response = restTemplate.getForEntity(
            "/api/v1/instances",
            InstanceListResponse.class
        );
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getTotal());
    }
    
    @Test
    void deleteInstance_returnsNoContent() {
        // Given
        User user = userRepository.save(new User("user@example.com", "password"));
        Instance instance = instanceRepository.save(
            new Instance("MYSQL", "db-1", user)
        );
        
        // When
        ResponseEntity<Void> response = restTemplate.exchange(
            "/api/v1/instances/" + instance.getId(),
            HttpMethod.DELETE,
            null,
            Void.class
        );
        
        // Then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertTrue(instanceRepository.findById(instance.getId()).isEmpty());
    }
}
```

---

## Contract Tests (Cross-Module)

### Testing Module APIs for Phase 2 Readiness

```java
// ✅ Test module contracts (becomes HTTP contract in Phase 2)
@SpringBootTest
@Transactional
public class PaymentModuleApiContractTest {
    
    @Autowired
    private PaymentModuleApi paymentApi;
    
    @Autowired
    private PlanRepository planRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void canUserCreateInstance_contractTest() {
        // This test documents the contract that Instance module depends on
        // In Phase 2, this becomes an HTTP contract test
        
        // Given
        User user = userRepository.save(new User("user@example.com", "password"));
        Plan freePlan = planRepository.save(new Plan("FREE", 2, 100));
        
        // When/Then - User can create 0 and 1 instances
        assertTrue(paymentApi.canUserCreateInstance(user.getId(), 0));
        assertTrue(paymentApi.canUserCreateInstance(user.getId(), 1));
        
        // When/Then - User cannot create 2nd instance (limit reached)
        assertFalse(paymentApi.canUserCreateInstance(user.getId(), 2));
    }
    
    @Test
    void getUserCurrentPlan_contractTest() {
        // Given
        User user = userRepository.save(new User("user@example.com", "password"));
        Plan expectedPlan = planRepository.save(new Plan("FREE", 2, 100));
        
        // When
        Plan result = paymentApi.getUserCurrentPlan(user.getId());
        
        // Then
        assertNotNull(result);
        assertEquals("FREE", result.getName());
        assertEquals(2, result.getMaxInstances());
    }
}
```

### Documentation of Contract

```java
/**
 * Contract for PaymentModuleApi - used by Instance module to validate plan limits.
 * 
 * In Phase 1: In-process interface (MockitoExtension)
 * In Phase 2: HTTP client (Feign) with same interface
 * 
 * Expected behavior:
 * - canUserCreateInstance(userId, count) returns true if count < plan.maxInstances
 * - getUserCurrentPlan(userId) returns the active plan for the user
 * - Throws PlanNotFoundException if user has no plan
 */
public interface PaymentModuleApi {
    boolean canUserCreateInstance(UUID userId, int currentInstanceCount);
    Plan getUserCurrentPlan(UUID userId);
}
```

---

## Event Listener Testing

### Testing Event Publishing and Listening

```java
@SpringBootTest
@Transactional
public class InstanceEventListenerTest {
    
    @Autowired
    private InstanceService instanceService;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @MockBean
    private NotificationService notificationService;
    
    @Test
    void onInstanceCreated_sendsNotification() {
        // Given
        User user = new User("user@example.com", "password");
        CreateInstanceRequest req = new CreateInstanceRequest(
            "MYSQL", "db-1", null
        );
        
        // When
        Instance instance = instanceService.createInstance(req, user);
        
        // Then - verify notification service was called
        verify(notificationService).sendInstanceCreatedEmail(
            user.getEmail(),
            instance.getName()
        );
    }
}
```

---

## Exception Testing

### Testing Exception Handling

```java
@ExtendWith(MockitoExtension.class)
public class InstanceServiceExceptionTest {
    
    @Mock
    private PaymentModuleApi paymentApi;
    
    @Mock
    private InstanceRepository instanceRepository;
    
    @InjectMocks
    private InstanceService service;
    
    @Test
    void createInstance_whenPlanLimitExceeded_throwsException() {
        // Given
        User user = new User("user@example.com", "password");
        CreateInstanceRequest req = new CreateInstanceRequest("MYSQL", "db-1", null);
        
        when(paymentApi.canUserCreateInstance(user.getId(), 2)).thenReturn(false);
        
        // When & Then
        PlanLimitExceededException ex = assertThrows(PlanLimitExceededException.class, () -> {
            service.createInstance(req, user);
        });
        
        assertEquals("PLAN_LIMIT_EXCEEDED", ex.getCode());
        assertTrue(ex.getDetails().containsKey("maxInstances"));
    }
    
    @Test
    void createInstance_whenInstanceNotFound_throwsException() {
        // Given
        User user = new User("user@example.com", "password");
        UUID id = UUID.randomUUID();
        
        when(instanceRepository.findById(id)).thenReturn(Optional.empty());
        
        // When & Then
        InstanceNotFoundException ex = assertThrows(InstanceNotFoundException.class, () -> {
            service.getById(id);
        });
        
        assertEquals("INSTANCE_NOT_FOUND", ex.getCode());
    }
}
```

---

## Validation Testing

### Testing DTO Validation

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
        // Given
        CreateInstanceRequest req = new CreateInstanceRequest(
            "MYSQL", "my-db", null
        );
        
        // When
        Set<ConstraintViolation<CreateInstanceRequest>> violations = validator.validate(req);
        
        // Then
        assertTrue(violations.isEmpty());
    }
    
    @Test
    void validate_withBlankName_fails() {
        // Given
        CreateInstanceRequest req = new CreateInstanceRequest(
            "MYSQL", "", null
        );
        
        // When
        Set<ConstraintViolation<CreateInstanceRequest>> violations = validator.validate(req);
        
        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }
    
    @Test
    void validate_withInvalidEngine_fails() {
        // Given
        CreateInstanceRequest req = new CreateInstanceRequest(
            "INVALID_ENGINE", "my-db", null
        );
        
        // When
        Set<ConstraintViolation<CreateInstanceRequest>> violations = validator.validate(req);
        
        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("engine")));
    }
}
```

---

## Repository Testing

### Testing Data Access Layer

```java
@DataJpaTest
public class InstanceRepositoryTest {
    
    @Autowired
    private InstanceRepository instanceRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void findByUserId_returnsUserInstances() {
        // Given
        User user = userRepository.save(new User("user@example.com", "password"));
        Instance i1 = instanceRepository.save(new Instance("MYSQL", "db-1", user));
        Instance i2 = instanceRepository.save(new Instance("POSTGRESQL", "db-2", user));
        
        // When
        List<Instance> results = instanceRepository.findByUserId(user.getId());
        
        // Then
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(i -> i.getId().equals(i1.getId())));
    }
    
    @Test
    void findByUserId_returnsEmptyForNoInstances() {
        // Given
        User user = userRepository.save(new User("user@example.com", "password"));
        
        // When
        List<Instance> results = instanceRepository.findByUserId(user.getId());
        
        // Then
        assertTrue(results.isEmpty());
    }
}
```

---

## Test Naming Convention

### Descriptive Test Names

```java
// ✅ GOOD: Describes what is being tested and expected outcome
void createInstance_whenPlanLimitExceeded_throwsException()
void deleteInstance_withValidId_returnsNoContent()
void listInstances_withValidPageParams_returnsPagedData()
void getInstanceLogs_withInvalidId_throws404()

// ❌ BAD: Vague names
void testCreate()
void test1()
void shouldWork()
void badTest()
```

---

## Best Practices

### ✅ DO: Arrange-Act-Assert (AAA) Pattern

```java
@Test
void createInstance_succeeds() {
    // Arrange - set up test data
    User user = new User("user@example.com", "password");
    CreateInstanceRequest req = new CreateInstanceRequest("MYSQL", "db-1", null);
    
    // Act - perform the action
    Instance instance = instanceService.createInstance(req, user);
    
    // Assert - verify results
    assertNotNull(instance.getId());
    assertEquals("MYSQL", instance.getEngine());
}
```

### ✅ DO: One Assertion per Test (Prefer)

```java
// ✅ GOOD: Focused test, easy to debug
@Test
void createInstance_returnsUniqueId() {
    Instance instance = instanceService.createInstance(req, user);
    assertNotNull(instance.getId());
}

@Test
void createInstance_setsCorrectEngine() {
    Instance instance = instanceService.createInstance(req, user);
    assertEquals("MYSQL", instance.getEngine());
}
```

### ✅ DO: Use @Transactional for Database Tests

```java
// ✅ Rollback changes after test
@SpringBootTest
@Transactional
public class InstanceServiceIntegrationTest {
    // Test methods automatically rolled back
}
```

### ❌ DON'T: Create Tests That Depend on Each Other

```java
// ❌ BAD: Tests should be independent
@Test
void test1_createUser() {
    userRepository.save(new User("user@test.com", "pass"));
}

@Test
void test2_getUserCreatedInTest1() {
    User user = userRepository.findByEmail("user@test.com");  // Assumes test1 ran first!
}

// ✅ GOOD: Each test is independent
@Test
void getUserByEmail_returnsUser() {
    User saved = userRepository.save(new User("user@test.com", "pass"));
    User found = userRepository.findByEmail("user@test.com");
    assertEquals(saved.getId(), found.getId());
}
```

### ❌ DON'T: Mock Everything

```java
// ❌ BAD: Over-mocking loses integration test value
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    
    @Mock private UserRepository repo;
    @Mock private EmailService email;
    @Mock private PlanService plan;
    @InjectMocks private UserService service;
    
    // Testing mocks, not the actual service behavior
}

// ✅ GOOD: Use integration tests for real interaction
@SpringBootTest
@Transactional
public class UserServiceIntegrationTest {
    @Autowired private UserService service;
    @Autowired private UserRepository repo;
    @MockBean private EmailService email;  // Mock only external service
    
    // Testing actual service behavior with real database
}
```

---

**Last Updated:** November 2025  
**Version:** 1.0
