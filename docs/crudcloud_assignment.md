# 🧭 CrudCloud – Cloud Database Management Platform

## Developer Assignment Document

---

## 📋 Overview

**CrudCloud** is a cloud-based platform that enables individuals and organizations to create, manage, and access real database instances running as Docker containers on a VPS. The platform automates database provisioning, provides secure credential management, and implements a subscription-based payment model with tiered service plans.

This is a **functional production-ready prototype** that demonstrates:
- Real container orchestration with Docker
- Payment gateway integration (Mercado Pago)
- Multi-tier subscription management
- Secure credential handling and rotation
- Automated notifications and PDF generation

---

## 🎯 High-Level Goal

Build and deploy a complete full-stack platform where users can:
1. Register and authenticate (individuals or organizations)
2. Create real database instances (as Docker containers) based on selected engines
3. Manage instances through their lifecycle (create, suspend, resume, delete)
4. Upgrade subscription plans via Mercado Pago integration
5. Receive and manage secure database credentials with one-time password visibility

**Key Deliverables:**
- Fully deployed backend and frontend on assigned VPS
- Working Docker container orchestration for 6 database engines
- Functional payment flow (Sandbox → Production)
- Complete documentation site
- Azure Boards traceability with Git Flow and Conventional Commits

---

## ⚙️ Technical Requirements & Constraints

### Stack

**Backend:**
- Java 17+ with Spring Boot 3.x
- Spring Data JPA + Hibernate (LAZY loading by default)
- Bean Validation for input validation
- Docker Java SDK for container orchestration
- Email service (SMTP configuration)
- PDF generation library (iText or Apache PDFBox)
- Mercado Pago SDK

**Frontend:**
- React 18+
- React Router for navigation
- State management (Context API or Redux)
- Axios or Fetch for API calls
- Form validation library

**Infrastructure:**
- Docker & Docker Compose
- VPS with Docker daemon accessible
- Nginx for reverse proxy
- SSL certificates for subdomains

### Architecture

```
backend/
├── src/main/java/com/crudzaso/crudcloud/
│   ├── controller/
│   │   ├── AuthController.java
│   │   ├── UserController.java
│   │   ├── InstanceController.java
│   │   ├── PlanController.java
│   │   └── PaymentController.java
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── UserService.java
│   │   ├── InstanceService.java
│   │   ├── DockerOrchestrationService.java
│   │   ├── CredentialService.java
│   │   ├── EmailService.java
│   │   ├── PdfService.java
│   │   └── PaymentService.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── OrganizationRepository.java
│   │   ├── InstanceRepository.java
│   │   ├── PlanRepository.java
│   │   └── TransactionRepository.java
│   ├── model/
│   │   ├── User.java
│   │   ├── Organization.java
│   │   ├── Instance.java
│   │   ├── Plan.java
│   │   ├── Transaction.java
│   │   └── InstanceStatus.java (enum)
│   ├── dto/
│   │   ├── request/
│   │   └── response/
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── DockerConfig.java
│   │   └── CorsConfig.java
│   └── exception/
│       ├── GlobalExceptionHandler.java
│       └── custom exceptions
├── Dockerfile
└── docker-compose.yml

frontend/
├── src/
│   ├── components/
│   │   ├── Auth/
│   │   ├── Dashboard/
│   │   ├── Instances/
│   │   ├── Plans/
│   │   └── common/
│   ├── pages/
│   │   ├── Login.jsx
│   │   ├── Register.jsx
│   │   ├── Dashboard.jsx
│   │   ├── EngineCatalog.jsx
│   │   ├── InstanceList.jsx
│   │   ├── InstanceDetail.jsx
│   │   └── Plans.jsx
│   ├── services/
│   │   ├── api.js
│   │   ├── authService.js
│   │   └── instanceService.js
│   ├── context/
│   │   └── AuthContext.jsx
│   └── utils/
├── Dockerfile
└── package.json
```

### Database Schema

**Users Table:**
- id (PK)
- email (unique)
- password (hashed)
- name
- user_type (INDIVIDUAL, ORGANIZATION)
- organization_id (FK, nullable)
- plan_id (FK)
- created_at
- updated_at

**Organizations Table:**
- id (PK)
- name
- admin_user_id (FK)

**Plans Table:**
- id (PK)
- name (FREE, STANDARD, PREMIUM)
- max_instances (2, 5, 10)
- price
- currency

**Instances Table:**
- id (PK)
- user_id (FK)
- engine (MySQL, PostgreSQL, MongoDB, Redis, Cassandra, SQL Server)
- database_name
- container_id
- host
- port
- username
- password_hash
- status (CREATING, RUNNING, SUSPENDED, DELETED)
- created_at
- updated_at

**Transactions Table:**
- id (PK)
- user_id (FK)
- plan_id (FK)
- amount
- currency
- payment_id (Mercado Pago ID)
- status (PENDING, APPROVED, REJECTED)
- created_at

### Naming Conventions

- **Repositories:** `crudcloud-frontend-name-team`, `crudcloud-backend-name-team`, `crudcloud-docs-name-team`
- **Subdomains:** 
  - Frontend: `name-team.crudzaso.com`
  - Backend: `api.name-team.crudzaso.com`
  - Docs: `docs.name-team.crudzaso.com`
- **Git:** Git Flow with Conventional Commits (feat:, fix:, docs:, etc.)

### Docker Container Ports

Allocate port ranges dynamically for each instance:
- MySQL: 3306
- PostgreSQL: 5432
- MongoDB: 27017
- Redis: 6379
- Cassandra: 9042
- SQL Server: 1433

Use port mapping to expose containers on unique ports per instance.

---

## 🧩 Functional Requirements

### 1. Authentication & User Management

#### Endpoints

| Method | Endpoint | Description | Request Body | Response |
|--------|----------|-------------|--------------|----------|
| POST | `/api/auth/register` | Register new user | `{email, password, name, userType}` | `{userId, token}` |
| POST | `/api/auth/login` | Authenticate user | `{email, password}` | `{token, user}` |
| GET | `/api/auth/profile` | Get user profile | - | `{user, plan}` |

**Validation Rules:**
- Email: valid format, unique
- Password: min 8 chars, 1 uppercase, 1 number, 1 special char
- UserType: INDIVIDUAL or ORGANIZATION

#### User Types
- **Individual:** Single user account
- **Organization:** Multiple users under one organization with shared plan

---

### 2. Database Engine Catalog

#### Available Engines

| Engine | Version | Default Port | Image |
|--------|---------|--------------|-------|
| MySQL | 8.0 | 3306 | `mysql:8.0` |
| PostgreSQL | 15 | 5432 | `postgres:15` |
| MongoDB | 7.0 | 27017 | `mongo:7.0` |
| Redis | 7.2 | 6379 | `redis:7.2-alpine` |
| Cassandra | 4.1 | 9042 | `cassandra:4.1` |
| SQL Server | 2022 | 1433 | `mcr.microsoft.com/mssql/server:2022-latest` |

#### Endpoint

| Method | Endpoint | Description | Response |
|--------|----------|-------------|----------|
| GET | `/api/engines` | List available engines | `[{name, version, description}]` |

---

### 3. Instance Management

#### Endpoints

| Method | Endpoint | Description | Request Body | Response |
|--------|----------|-------------|--------------|----------|
| POST | `/api/instances` | Create new instance | `{engine, databaseName?}` | `{instance, credentials}` |
| GET | `/api/instances` | List user instances | - | `[{id, engine, status, createdAt}]` |
| GET | `/api/instances/{id}` | Get instance details | - | `{instance, connectionInfo}` |
| PUT | `/api/instances/{id}/suspend` | Suspend instance | - | `{status: "SUSPENDED"}` |
| PUT | `/api/instances/{id}/resume` | Resume instance | - | `{status: "RUNNING"}` |
| DELETE | `/api/instances/{id}` | Delete instance | - | `{message}` |
| POST | `/api/instances/{id}/rotate-password` | Rotate password | - | `{newPassword}` |
| GET | `/api/instances/{id}/credentials/pdf` | Download credentials PDF | - | Binary PDF |

#### Instance Lifecycle

```
CREATING → RUNNING → SUSPENDED → RUNNING
                  ↓
                DELETED
```

#### Create Instance Flow

1. **Validate plan limits:** Check if user can create more instances
2. **Generate database name:** 
   - FREE plan: auto-generate (e.g., `db_a1b2c3d4`)
   - STANDARD/PREMIUM: use provided name or generate
3. **Generate credentials:**
   - Username: `user_<hash>`
   - Password: secure random 16-char string
   - Port: allocate from available pool
4. **Create Docker container:**
   ```bash
   docker run -d \
     --name crud_instance_{id} \
     -e MYSQL_ROOT_PASSWORD={password} \
     -e MYSQL_DATABASE={dbName} \
     -p {port}:3306 \
     mysql:8.0
   ```
5. **Save to database:** Store instance with CREATING status
6. **Wait for container:** Poll container health
7. **Update status:** Set to RUNNING
8. **Send notification:** Email with connection info (no password in email)
9. **Return response:**
   ```json
   {
     "instanceId": "uuid",
     "engine": "MySQL",
     "databaseName": "db_a1b2c3d4",
     "host": "api.name-team.crudzaso.com",
     "port": 33061,
     "username": "user_xyz123",
     "password": "TH1sI5Sh0wnOnc3!",
     "status": "RUNNING",
     "createdAt": "2025-11-09T10:30:00Z",
     "message": "⚠️ Save this password! It won't be shown again."
   }
   ```

#### Suspend Instance

- Stop Docker container: `docker stop crud_instance_{id}`
- Update status to SUSPENDED
- Container remains but doesn't consume resources

#### Resume Instance

- Start Docker container: `docker start crud_instance_{id}`
- Update status to RUNNING

#### Delete Instance

- Stop and remove container: `docker rm -f crud_instance_{id}`
- Update status to DELETED (soft delete) or hard delete record
- Free allocated port

#### Rotate Password

1. Generate new password
2. Update database credentials in container
3. Update password_hash in database
4. Send email notification with new password
5. Return new password **once**

---

### 4. Plans & Limits

#### Plan Definitions

| Plan | Max Instances | Price (Monthly) | Features |
|------|---------------|-----------------|----------|
| FREE | 2 | $0 | Auto-generated DB names |
| STANDARD | 5 | $19.99 | Custom DB names, Email support |
| PREMIUM | 10 | $49.99 | All STANDARD + Priority support |

#### Endpoints

| Method | Endpoint | Description | Response |
|--------|----------|-------------|----------|
| GET | `/api/plans` | List available plans | `[{name, maxInstances, price}]` |
| GET | `/api/plans/current` | Get user's current plan | `{plan, usage}` |

#### Limit Enforcement

- Before creating instance, check: `currentInstances < plan.maxInstances`
- Return 403 Forbidden if limit reached:
  ```json
  {
    "error": "Instance limit reached",
    "message": "Your FREE plan allows up to 2 instances. Upgrade to create more.",
    "currentPlan": "FREE",
    "instanceCount": 2,
    "maxInstances": 2
  }
  ```

---

### 5. Payment & Subscriptions

#### Mercado Pago Integration

**Flow:**
1. User selects plan upgrade
2. Backend creates Mercado Pago preference
3. User redirects to Mercado Pago checkout
4. After payment, Mercado Pago sends webhook notification
5. Backend verifies payment and updates user plan
6. User receives confirmation email

#### Endpoints

| Method | Endpoint | Description | Request Body | Response |
|--------|----------|-------------|--------------|----------|
| POST | `/api/payments/create-preference` | Create payment preference | `{planId}` | `{preferenceId, initPoint}` |
| POST | `/api/payments/webhook` | Handle Mercado Pago webhook | Mercado Pago payload | `200 OK` |
| GET | `/api/payments/history` | Get user transaction history | - | `[{amount, date, status}]` |

#### Payment States

- **PENDING:** Payment initiated
- **APPROVED:** Payment successful, plan upgraded
- **REJECTED:** Payment failed, plan unchanged
- **CANCELLED:** User cancelled payment

#### Sandbox Testing

- Use Mercado Pago test credentials
- Test cards provided in Mercado Pago docs
- Verify webhook handling before production

---

### 6. Notifications

#### Email Templates

**1. Instance Created**
```
Subject: Your {engine} database is ready!

Hi {userName},

Your new {engine} database instance has been created successfully.

Connection Details:
- Host: {host}
- Port: {port}
- Database: {databaseName}
- Username: {username}

⚠️ Important: Your password was shown once during creation. 
You can download it from your dashboard or rotate it if needed.

Dashboard: https://name-team.crudzaso.com/instances/{id}
```

**2. Password Rotated**
```
Subject: Database password updated

Hi {userName},

The password for your {engine} instance ({databaseName}) has been rotated.

New Password: {newPassword}

⚠️ Save this password! It won't be shown again.
```

**3. Plan Upgraded**
```
Subject: Welcome to {planName}!

Hi {userName},

Your subscription has been upgraded to {planName}.

Benefits:
- Up to {maxInstances} database instances
- {features}

Thank you for choosing CrudCloud!
```

#### PDF Generation

When user clicks "Download Credentials":
- Generate PDF with instance details including password
- Include QR code for connection string (optional)
- Add warning: "Store this document securely"

---

### 7. Frontend Views

#### Pages & Components

**1. Authentication**
- `/login` - Login form
- `/register` - Registration form with user type selection

**2. Dashboard**
- `/dashboard` - Overview with:
  - Current plan and usage (e.g., "2/5 instances used")
  - Quick stats
  - Recent instances
  - Plan upgrade CTA

**3. Engine Catalog**
- `/engines` - Grid of available database engines with descriptions

**4. Instances**
- `/instances` - Table listing all instances with:
  - Engine icon
  - Database name
  - Status badge
  - Actions (View, Suspend/Resume, Delete)
- `/instances/new` - Create instance form:
  - Engine selector
  - Database name input (disabled for FREE plan)
  - Create button (disabled if limit reached)
- `/instances/{id}` - Instance detail page:
  - Connection information
  - Status indicator
  - Actions: Suspend/Resume, Rotate Password, Delete, Download PDF
  - ⚠️ Password only shown immediately after creation or rotation

**5. Plans**
- `/plans` - Plan comparison table with upgrade buttons

**6. Settings**
- `/settings` - User profile and organization management

#### UI/UX Requirements

- **Responsive design:** Mobile-first approach
- **Loading states:** Spinners during async operations
- **Success/Error notifications:** Toast or snackbar for user feedback
- **Confirmation dialogs:** Before destructive actions (delete, rotate password)
- **Copy to clipboard:** For credentials
- **Status indicators:** Color-coded badges (green=RUNNING, yellow=SUSPENDED, red=DELETED)

---

## 🧪 Testing Requirements

### Backend Tests

**Unit Tests (JUnit 5 + Mockito):**
- Service layer logic
- DTO validation
- Password generation and hashing
- Plan limit calculations

**Integration Tests:**
- Controller endpoints with MockMvc
- Repository queries with @DataJpaTest
- Docker orchestration (mocked or testcontainers)

**Minimum Coverage:** 70%

**Example Test Cases:**
```java
@Test
void createInstance_whenLimitReached_shouldThrowException() {
    // Given: User has 2 instances on FREE plan
    // When: Attempt to create 3rd instance
    // Then: Throw PlanLimitExceededException
}

@Test
void rotatePassword_shouldInvalidateOldPassword() {
    // Given: Instance with password A
    // When: Rotate password to B
    // Then: Old password A should not work
}
```

### Frontend Tests

- Component unit tests (Jest + React Testing Library)
- Form validation tests
- API service mocks

---

## ⚠️ Error Handling

### Global Exception Handler

Use `@ControllerAdvice` to standardize error responses:

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(PlanLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handlePlanLimit(PlanLimitExceededException ex) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse(
                "PLAN_LIMIT_EXCEEDED",
                ex.getMessage(),
                Map.of("currentPlan", ex.getPlan(), "maxInstances", ex.getLimit())
            ));
    }
}
```

### Standard Error Response

```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable message",
  "details": {
    "field": "value"
  },
  "timestamp": "2025-11-09T10:30:00Z"
}
```

### HTTP Status Codes

| Code | Usage |
|------|-------|
| 200 | Successful GET/PUT |
| 201 | Resource created |
| 400 | Validation error |
| 401 | Unauthorized |
| 403 | Forbidden (e.g., plan limit) |
| 404 | Resource not found |
| 409 | Conflict (e.g., duplicate email) |
| 500 | Server error |

---

## 🔐 Security

### Authentication

- **JWT tokens** for stateless authentication
- Token expiration: 24 hours
- Refresh token mechanism (optional)

### Password Management

- **Hashing:** BCrypt with salt
- **One-time visibility:** Password only returned once in API response
- **Rotation:** Generates new secure password, invalidates old

### Credential Storage

- **Never store passwords in plain text**
- **Database:** Store hashed passwords
- **Environment variables:** For Docker container credentials
- **Encryption at rest:** For sensitive data (optional)

### CORS Configuration

```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(source -> {
            config.setAllowedOrigins(List.of("https://name-team.crudzaso.com"));
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
            config.setAllowCredentials(true);
        });
    }
}
```

### Docker Security

- Run containers with limited privileges
- Use Docker secrets for sensitive data (production)
- Implement resource limits (CPU, memory)

---

## 📦 Deliverables

### Code Repositories

1. **Backend Repository:** `crudcloud-backend-name-team`
   - Source code with complete implementation
   - Dockerfile and docker-compose.yml
   - application.properties with environment variable references
   - README.md with setup instructions

2. **Frontend Repository:** `crudcloud-frontend-name-team`
   - React application source code
   - Dockerfile
   - Environment configuration
   - README.md

3. **Documentation Repository:** `crudcloud-docs-name-team`
   - Docusaurus site
   - API documentation
   - User guides
   - Architecture diagrams
   - Setup and deployment guides

### Deployment

- All services running on VPS
- Accessible via configured subdomains
- SSL certificates installed
- Docker containers for databases operational

### Documentation Site

Published at `docs.name-team.crudzaso.com` with:
- Architecture overview
- API reference (Swagger/OpenAPI)
- User manual
- Developer setup guide
- Deployment procedures

### Project Management

- **Azure Boards:** Complete traceability
  - User stories for each feature
  - Tasks linked to stories
  - Bugs tracked and resolved
  - All work items linked to commits and PRs
- **Git Flow:** Feature branches, develop, main
- **Conventional Commits:** All commits follow convention

---

## ✅ Acceptance Criteria

### Functional

- [ ] User registration and login working for individuals and organizations
- [ ] All 6 database engines can be provisioned as real Docker containers
- [ ] Credentials generated and returned with one-time password visibility
- [ ] PDF download with credentials functions correctly
- [ ] Email notifications sent for all required events
- [ ] Password rotation working and invalidates old credentials
- [ ] Plan limits enforced (2/5/10 instances)
- [ ] Cannot create instance when limit reached
- [ ] Suspend/resume instance changes container state
- [ ] Delete instance removes container and frees resources
- [ ] Mercado Pago Sandbox integration complete
- [ ] Payment webhook updates user plan correctly
- [ ] Transaction history visible to users

### Technical

- [ ] Backend follows layered architecture (controller→service→repository)
- [ ] Spring JPA with LAZY loading configured
- [ ] Bean Validation on all DTOs
- [ ] Global exception handler with standardized errors
- [ ] ResponseEntity used in all controllers
- [ ] Frontend React application responsive and functional
- [ ] Docker containers for backend and frontend
- [ ] All services deployed on VPS with correct subdomains
- [ ] SSL certificates installed and HTTPS working
- [ ] Unit and integration tests with ≥70% coverage

### Documentation & Process

- [ ] Docusaurus site published with complete documentation
- [ ] API documented with examples
- [ ] Azure Boards shows complete traceability
- [ ] Git Flow followed with conventional commits
- [ ] README files present in all repositories
- [ ] Setup instructions validated by fresh deployment

---

## 📘 Scope Summary

**Project Type:** Functional production-ready prototype

**Learning Goals:**
- Cloud infrastructure management
- Container orchestration with Docker
- Payment gateway integration
- Secure credential management
- Full-stack development with modern frameworks
- CI/CD and deployment practices

**Production vs. Prototype:**
- This is a **working prototype** meant to demonstrate real functionality
- Docker containers must actually run on the VPS
- Payment integration uses real Mercado Pago APIs (Sandbox first)
- Can be extended to production with additional security hardening and scalability

**Out of Scope:**
- Auto-scaling or Kubernetes orchestration
- Advanced monitoring and alerting
- Backup and disaster recovery automation
- Multi-region deployment
- Advanced IAM with roles and permissions

---

## ⚙️ Non-Functional Requirements

### Code Quality

- **Clean Code:** Follow SOLID principles
- **Naming:** Clear, descriptive variable and method names
- **Comments:** Javadoc for public methods, inline for complex logic
- **Formatting:** Consistent code style (use IDE formatter)
- **DRY:** Avoid code duplication

### Performance

- **Response Time:** API endpoints < 500ms (excluding container startup)
- **Container Startup:** < 30 seconds for database availability
- **Concurrency:** Handle multiple simultaneous instance creations

### Reliability

- **Error Recovery:** Graceful handling of Docker failures
- **Idempotency:** Payment webhooks can be safely retried
- **Data Integrity:** Transactions for critical operations

### Maintainability

- **Modular Design:** Clear separation of concerns
- **Configuration:** Externalize all environment-specific values
- **Logging:** Structured logging with appropriate levels (INFO, ERROR, DEBUG)
- **Documentation:** Keep README and docs updated with code changes

### Security

- **Input Validation:** All user inputs validated
- **SQL Injection:** Use parameterized queries (JPA handles this)
- **XSS Protection:** Sanitize outputs in frontend
- **Rate Limiting:** Consider API rate limits for production

---

## 🔄 Development Workflow

### Git Flow

```
main (production)
  ↑
develop
  ↑
feature/ABC-123-feature-name
```

**Branch Naming:**
- Features: `feature/ABC-123-short-description`
- Bugs: `bugfix/ABC-123-short-description`
- Hotfixes: `hotfix/ABC-123-short-description`

### Commit Convention

```
type(scope): subject

body (optional)

footer (optional)
```

**Types:** feat, fix, docs, style, refactor, test, chore

**Examples:**
```
feat(instances): add password rotation endpoint

Implement POST /instances/{id}/rotate-password endpoint that
generates new secure password and invalidates the old one.

Closes ABC-123
```

### Pull Request Process

1. Create feature branch from `develop`
2. Implement feature with tests
3. Commit using conventional commits
4. Push and create PR to `develop`
5. Link PR to Azure Boards work item
6. Code review and approval
7. Merge to `develop`
8. Deploy to staging (develop branch)
9. Merge to `main` for production

---

## 🚀 Deployment Checklist

### Pre-Deployment

- [ ] Environment variables configured
- [ ] Database migrations ready
- [ ] Docker images built and tested
- [ ] SSL certificates obtained
- [ ] DNS records configured
- [ ] Mercado Pago credentials (Sandbox/Production)

### Deployment Steps

1. **Backend:**
   ```bash
   docker build -t crudcloud-backend .
   docker run -d -p 8080:8080 \
     -e DB_URL=... \
     -e MERCADO_PAGO_TOKEN=... \
     --name crudcloud-backend \
     crudcloud-backend
   ```

2. **Frontend:**
   ```bash
   docker build -t crudcloud-frontend .
   docker run -d -p 3000:80 \
     -e REACT_APP_API_URL=https://api.name-team.crudzaso.com \
     --name crudcloud-frontend \
     crudcloud-frontend
   ```

3. **Nginx Reverse Proxy:**
   Configure virtual hosts for subdomains

4. **Docusaurus:**
   Deploy static site to docs subdomain

### Post-Deployment

- [ ] Smoke tests passed
- [ ] Create test instance verified
- [ ] Payment flow tested (Sandbox)
- [ ] Email notifications received
- [ ] Documentation accessible
- [ ] Monitoring configured

---

## 📚 Additional Resources

### Docker SDK Documentation
- [Docker Java SDK](https://github.com/docker-java/docker-java)

### Mercado Pago
- [API Documentation](https://www.mercadopago.com.ar/developers)
- [Sandbox Testing](https://www.mercadopago.com.ar/developers/en/guides/testing)

### Spring Boot
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)

### React
- [React Documentation](https://react.dev/)
- [React Router](https://reactrouter.com/)

---

## 📝 Summary of Transformations

This assignment document was created by analyzing and restructuring the original Spanish statement with the following enhancements:

**Added Sections:**
- Complete API endpoint specifications with request/response examples
- Detailed database schema
- Docker container orchestration specifications
- Email templates and PDF generation requirements
- Frontend page-by-page specifications
- Comprehensive error handling standards
- Security requirements and best practices
- Non-functional requirements (performance, reliability, maintainability)
- Development workflow with Git Flow and Conventional Commits
- Deployment checklist and procedures
- Testing requirements with coverage expectations

**Clarifications Made:**
- Exact technology stack versions
- Package/folder structure for both frontend and backend
- Instance lifecycle state machine
- Plan limits and enforcement logic
- Payment flow step-by-step
- One-time password visibility implementation
- Container port allocation strategy

**Professional Enhancements:**
- Emoji-based section headers for easy navigation
- Markdown tables for structured data
- Code examples for critical implementations
- Acceptance criteria checklist
- Resource links for all major technologies
- Complete scope definition (in-scope vs. out-of-scope)

This document is now ready for development team implementation or AI-assisted code generation.