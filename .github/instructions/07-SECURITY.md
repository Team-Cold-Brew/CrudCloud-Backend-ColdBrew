# 🔐 Security Best Practices

## Authentication & Authorization

### JWT Token Strategy

```java
// ✅ Use JWT for stateless authentication
@Service
@Slf4j
public class TokenService {
    
    @Value("${jwt.secret}")
    private String secretKey;
    
    @Value("${jwt.expiration:86400000}")  // 24 hours default
    private Long expiration;
    
    public String generateToken(User user) {
        return Jwts.builder()
            .setSubject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("roles", user.getRoles())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(SignatureAlgorithm.HS512, secretKey)
            .compact();
    }
    
    public UUID validateAndGetUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody();
            return UUID.fromString(claims.getSubject());
        } catch (JwtException e) {
            logger.warn("Invalid JWT token", e);
            throw new AuthenticationException("Invalid or expired token");
        } catch (IllegalArgumentException e) {
            logger.warn("JWT claims string is empty", e);
            throw new AuthenticationException("Token is malformed");
        }
    }
    
    public String refreshToken(String token) {
        UUID userId = validateAndGetUserId(token);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        return generateToken(user);
    }
}
```

### JWT Spring Security Filter

```java
// ✅ Integrate JWT with Spring Security
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private TokenService tokenService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) 
            throws ServletException, IOException {
        try {
            String token = extractToken(request);
            if (token != null && !token.isEmpty()) {
                UUID userId = tokenService.validateAndGetUserId(token);
                
                // Create authentication
                UsernamePasswordAuthenticationToken auth = 
                    new UsernamePasswordAuthenticationToken(userId, null, new ArrayList<>());
                
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (AuthenticationException e) {
            logger.error("Authentication failed", e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
            return;
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}

// ✅ Register filter in SecurityConfiguration
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeRequests()
                .antMatchers("/api/v1/auth/**").permitAll()
                .antMatchers("/api/v1/payments/webhook").permitAll()  // MercadoPago webhooks
                .anyRequest().authenticated()
            .and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .addFilterBefore(new JwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

---

## Password Security

### Password Hashing with BCrypt

```java
// ✅ Use BCrypt for password hashing
@Configuration
public class SecurityConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);  // 12 rounds
    }
}

// ✅ Hash password when saving user
@Service
@Transactional
public class UserService {
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private UserRepository userRepository;
    
    public User registerUser(RegisterRequest req) {
        // Check if user already exists
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException(req.getEmail());
        }
        
        // Hash password before saving
        String hashedPassword = passwordEncoder.encode(req.getPassword());
        
        User user = new User();
        user.setEmail(req.getEmail());
        user.setPassword(hashedPassword);
        
        return userRepository.save(user);
    }
    
    public boolean validatePassword(User user, String plainPassword) {
        return passwordEncoder.matches(plainPassword, user.getPassword());
    }
}

// ✅ Validate password on login
@Service
public class AuthService {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private TokenService tokenService;
    
    public LoginResponse login(LoginRequest req) {
        User user = userService.findByEmail(req.getEmail())
            .orElseThrow(() -> new AuthenticationException("Invalid credentials"));
        
        if (!userService.validatePassword(user, req.getPassword())) {
            throw new AuthenticationException("Invalid credentials");
        }
        
        String token = tokenService.generateToken(user);
        return new LoginResponse(token, user.getId());
    }
}
```

### Password Requirements

```java
@Data
public class RegisterRequest {
    
    @NotBlank(message = "Password is required")
    @Length(min = 8, message = "Password must be at least 8 characters")
    private String password;
    
    @Override
    public String toString() {
        // ❌ NEVER: return password in toString
        return "RegisterRequest{email='" + email + "'}";
    }
}
```

---

## Sensitive Data Handling

### Never Expose Passwords in Responses

```java
// ✅ Response DTO without password
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID userId;
    private String email;
    private String fullName;
    
    // ❌ NEVER include:
    // private String password;
    // private String passwordHash;
}

// ✅ Exception: Show password ONLY on creation (one-time)
@Data
public class InstanceCreatedResponse {
    private UUID instanceId;
    private String engine;
    private String username;
    private String password;  // Shown ONLY here, one time
    private String connectionString;
    
    public InstanceCreatedResponse(Instance instance, String plainPassword) {
        this.instanceId = instance.getId();
        this.engine = instance.getEngine();
        this.username = instance.getUsername();
        this.password = plainPassword;  // Temporary, only for this response
        this.connectionString = instance.getConnectionString();
    }
}
```

### Never Log Sensitive Data

```java
// ✅ GOOD: Log safe information
logger.info("User registered: {}", user.getEmail());
logger.info("Instance created: {} by user: {}", instance.getId(), user.getId());

// ❌ BAD: Logging sensitive data
logger.info("User credentials: email={}, password={}", email, password);
logger.debug("Connection string: {}", "jdbc:mysql://user:password@host");
logger.error("Payment failed: card={}", creditCard.getCardNumber());

// ✅ GOOD: Use redaction helper
public static String redact(String value) {
    if (value == null || value.length() <= 4) return "****";
    return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
}

logger.info("Failed login attempt for: {}", redact(email));
logger.debug("Connection attempt: {}", redact(connectionString));
```

---

## Database Security

### Parameterized Queries (JPA)

```java
// ✅ JPA automatically uses parameterized queries
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);  // Parameterized
}

// ✅ Even safer with @Query
@Repository
public interface InstanceRepository extends JpaRepository<Instance, UUID> {
    
    @Query("SELECT i FROM Instance i WHERE i.user.id = :userId AND i.deletedAt IS NULL")
    List<Instance> findActiveByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT i FROM Instance i WHERE i.engine = :engine AND i.status = :status")
    List<Instance> findByEngineAndStatus(
        @Param("engine") String engine,
        @Param("status") InstanceStatus status
    );
}

// ❌ NEVER: String concatenation (SQL injection vulnerability)
String query = "SELECT * FROM users WHERE email = '" + email + "'";  // VULNERABLE!
```

### Soft Deletes

```sql
-- ✅ Use soft deletes for audit trail and data recovery
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP NULL;
ALTER TABLE instance ADD COLUMN deleted_at TIMESTAMP NULL;

-- ✅ Always filter soft-deleted records
SELECT * FROM instance WHERE deleted_at IS NULL;

-- ✅ Create index for performance
CREATE INDEX idx_instance_deleted_at ON instance(deleted_at);
```

```java
// ✅ Filter in queries
@Repository
public interface InstanceRepository extends JpaRepository<Instance, UUID> {
    
    @Query("SELECT i FROM Instance i WHERE i.user.id = :userId AND i.deletedAt IS NULL")
    List<Instance> findActiveByUserId(@Param("userId") UUID userId);
}

// ✅ Soft delete operation
@Service
public class InstanceService {
    
    public void delete(UUID instanceId) {
        Instance instance = instanceRepository.findById(instanceId)
            .orElseThrow(() -> new InstanceNotFoundException(instanceId));
        
        instance.setDeletedAt(Instant.now());
        instanceRepository.save(instance);
        
        logger.info("Instance soft-deleted: {}", instanceId);
    }
}
```

---

## API Security

### CORS Configuration

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("https://app.crudcloud.com", "https://www.crudcloud.com")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("Content-Type", "Authorization")
            .exposedHeaders("X-Total-Count")
            .allowCredentials(false)
            .maxAge(3600);
    }
}
```

### Rate Limiting

```java
// Consider adding rate limiting for production
// Example: Spring Cloud Gateway or Resilience4j

@Component
public class RateLimitingFilter implements Filter {
    
    private final RateLimiter rateLimiter = RateLimiter.create(100);  // 100 req/sec
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                        FilterChain chain) throws IOException, ServletException {
        if (!rateLimiter.tryAcquire()) {
            ((HttpServletResponse) response).sendError(429, "Too many requests");
            return;
        }
        chain.doFilter(request, response);
    }
}
```

### Input Validation

```java
// ✅ Validate all user input at boundary
@PostMapping
public ResponseEntity<InstanceResponse> createInstance(
    @Valid @RequestBody CreateInstanceRequest req
) {
    // @Valid ensures all constraints are checked
    // GlobalExceptionHandler catches validation errors
}

// ✅ Validate sensitive operations
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteInstance(
    @PathVariable UUID id,
    @RequestHeader("Authorization") String token
) {
    User user = authenticate(token);
    Instance instance = instanceRepository.findById(id)
        .orElseThrow(() -> new InstanceNotFoundException(id));
    
    // Verify authorization: only owner or admin can delete
    if (!isOwnerOrAdmin(user, instance)) {
        throw new AuthorizationException("Not authorized to delete this instance");
    }
    
    instanceService.delete(id);
    return ResponseEntity.noContent().build();
}
```

---

## Webhook Security (MercadoPago)

```java
// ✅ Verify webhook signature
@Service
public class PaymentWebhookService {
    
    @Value("${mercadopago.signing-key}")
    private String signingKey;
    
    public void processWebhook(String payload, String signature) {
        // Verify signature to prevent spoofing
        if (!verifySignature(payload, signature)) {
            throw new PaymentProcessingException("Invalid webhook signature");
        }
        
        // Parse and process webhook
        WebhookPayload data = parsePayload(payload);
        updateTransaction(data);
    }
    
    private boolean verifySignature(String payload, String signature) {
        String expectedSignature = generateSignature(payload);
        return MessageDigest.isEqual(
            signature.getBytes(),
            expectedSignature.getBytes()
        );
    }
    
    private String generateSignature(String payload) {
        // Use HMAC-SHA256
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(signingKey.getBytes(), "HmacSHA256");
        mac.init(keySpec);
        byte[] bytes = mac.doFinal(payload.getBytes());
        return Base64.getEncoder().encodeToString(bytes);
    }
}

// ✅ Webhook endpoint
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    
    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
        @RequestBody String payload,
        @RequestHeader("X-Signature") String signature
    ) {
        paymentWebhookService.processWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}
```

---

## Environment Variables

### Configuration Management

```yaml
# ✅ application.properties - NEVER commit secrets
spring.datasource.url=jdbc:postgresql://localhost:5432/crudcloud
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

jwt.secret=${JWT_SECRET}
jwt.expiration=${JWT_EXPIRATION:86400000}

mercadopago.access-token=${MERCADOPAGO_ACCESS_TOKEN}
mercadopago.signing-key=${MERCADOPAGO_SIGNING_KEY}

# ✅ .env.example - commit this with placeholders
DB_USERNAME=db_user
DB_PASSWORD=your_password_here
JWT_SECRET=your_jwt_secret_here
```

### Local Development

```bash
# ✅ Create .env.local (add to .gitignore)
DB_USERNAME=dev_user
DB_PASSWORD=dev_password_123
JWT_SECRET=dev_jwt_secret_key

# Load environment variables
export $(cat .env.local | xargs)

# Run Spring Boot
mvn spring-boot:run
```

---

## Best Practices Checklist

- ✅ Never store passwords in plain text (use BCrypt)
- ✅ Always validate user input (use @Valid, Bean Validation)
- ✅ Use HTTPS for all communication
- ✅ Use JWT tokens with appropriate expiration
- ✅ Never expose sensitive data in responses
- ✅ Never log passwords, tokens, or PII
- ✅ Use parameterized queries (JPA handles this)
- ✅ Verify webhook signatures from external services
- ✅ Implement soft deletes for audit trail
- ✅ Use environment variables for secrets
- ✅ Implement CORS properly for frontend
- ✅ Use rate limiting for public endpoints

---

**Last Updated:** November 2025  
**Version:** 1.0
