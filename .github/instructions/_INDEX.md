# 📚 CrudCloud Copilot Instructions - Index

Welcome to the CrudCloud Copilot Instructions! This is the split, modular version of the comprehensive architectural guidance for the CrudCloud project.

---

## 📖 Quick Navigation

### 🏗️ **Architecture & Fundamentals**
- **[01-ARCHITECTURE-OVERVIEW.md](01-ARCHITECTURE-OVERVIEW.md)** - Start here!
  - Project overview and two-phase architectural approach
  - Module structure and separation of concerns
  - Key rules for module organization

### 🔄 **Communication Patterns**
- **[02-MODULE-COMMUNICATION.md](02-MODULE-COMMUNICATION.md)** - How modules talk to each other
  - Event-driven communication (async, loosely coupled)
  - Module API contracts (sync, type-safe queries)
  - Cross-module communication workflows
  - When to use events vs. APIs

### ⚠️ **Exception Handling**
- **[03-EXCEPTION-HANDLING.md](03-EXCEPTION-HANDLING.md)** - Error management strategy
  - Exception hierarchy (all extend `ApiException`)
  - Global exception handler patterns
  - Module-specific exceptions
  - Best practices and testing

### 📮 **DTOs & Validation**
- **[04-DTO-AND-VALIDATION.md](04-DTO-AND-VALIDATION.md)** - Input/output contracts
  - Request DTOs with Bean Validation
  - Response DTOs with data protection
  - Special cases (creation responses with sensitive data)
  - Validation annotations and custom validators

### 🔌 **REST API Endpoints**
- **[05-API-ENDPOINTS.md](05-API-ENDPOINTS.md)** - RESTful design guidelines
  - Consistent versioned API paths (`/api/v1/`)
  - HTTP status codes (201, 400, 403, 404, etc.)
  - CRUD operations and custom actions
  - Request parameters (path, query, body, headers)

### 🧪 **Testing Guidelines**
- **[06-TESTING.md](06-TESTING.md)** - Testing strategy and patterns
  - Unit tests (Mockito, @ExtendWith)
  - Integration tests (@SpringBootTest)
  - Contract tests (module API validation)
  - Event listener testing
  - Exception and validation testing

### 🔐 **Security Best Practices**
- **[07-SECURITY.md](07-SECURITY.md)** - Authentication, authorization, data protection
  - JWT token generation and validation
  - Spring Security integration
  - Password hashing (BCrypt)
  - Sensitive data protection
  - Webhook signature verification
  - Environment variables for secrets

### 📝 **Code Quality Standards**
- **[08-CODE-QUALITY.md](08-CODE-QUALITY.md)** - Naming, style, and best practices
  - Naming conventions (classes, methods, variables)
  - Javadoc requirements
  - Code style (Lombok, method organization, exceptions)
  - Null safety and logging
  - DRY principle and immutability

### 🚀 **Phase 2: Microservices Preparation**
- **[09-PHASE-2-PREPARATION.md](09-PHASE-2-PREPARATION.md)** - Build with the future in mind
  - Module APIs → HTTP clients (Feign)
  - Events → Message queue (Kafka/RabbitMQ)
  - Eventual consistency design
  - Idempotency patterns
  - Transaction boundaries
  - Data storage strategy

---

## 🎯 Quick Reference by Task

### Starting a New Feature
1. Read: [01-ARCHITECTURE-OVERVIEW.md](01-ARCHITECTURE-OVERVIEW.md) - Understand module structure
2. Read: [02-MODULE-COMMUNICATION.md](02-MODULE-COMMUNICATION.md) - Understand dependencies
3. Read: [04-DTO-AND-VALIDATION.md](04-DTO-AND-VALIDATION.md) - Design request/response
4. Read: [05-API-ENDPOINTS.md](05-API-ENDPOINTS.md) - Design REST endpoints

### Implementing Business Logic
1. Read: [02-MODULE-COMMUNICATION.md](02-MODULE-COMMUNICATION.md) - Cross-module calls
2. Read: [03-EXCEPTION-HANDLING.md](03-EXCEPTION-HANDLING.md) - Error handling
3. Read: [09-PHASE-2-PREPARATION.md](09-PHASE-2-PREPARATION.md) - Design for scale

### Writing Tests
1. Read: [06-TESTING.md](06-TESTING.md) - All testing patterns
2. Reference: [03-EXCEPTION-HANDLING.md](03-EXCEPTION-HANDLING.md) - Exception testing

### Security Review
1. Read: [07-SECURITY.md](07-SECURITY.md) - All security concerns
2. Reference: [04-DTO-AND-VALIDATION.md](04-DTO-AND-VALIDATION.md) - Input validation

### Code Review
1. Read: [08-CODE-QUALITY.md](08-CODE-QUALITY.md) - Quality standards
2. Reference: [02-MODULE-COMMUNICATION.md](02-MODULE-COMMUNICATION.md) - Module boundaries
3. Reference: [07-SECURITY.md](07-SECURITY.md) - Security checklist

### Phase 2 Planning
1. Read: [09-PHASE-2-PREPARATION.md](09-PHASE-2-PREPARATION.md) - Microservices readiness

---

## 📋 Module Overview

### Modules in CrudCloud

```
┌─────────────────────────────────────────────┐
│          CrudCloud Backend (Phase 1)        │
├─────────────────────────────────────────────┤
│                   CORE                       │
│  • Config • Exceptions • Events • Utils     │
├─────────────────────────────────────────────┤
│    AUTH    │  INSTANCE  │  PAYMENT  │ PLAN  │
│ ┌────────┐ │ ┌────────┐ │┌────────┐│┌─────┐│
│ │ Users  │ │ │Docker  │ │││Payment││││Plans││
│ │ Tokens │ │ │Manage  │ │││Process││││ &   ││
│ │ JWT    │ │ │Orchestr.││││Billing│││Limits││
│ └────────┘ │ └────────┘ │└────────┘│└─────┘│
└─────────────────────────────────────────────┘
                      ↓
                  CATALOG
        Database Engine Information
```

### Communication Between Modules

- **Events** (Async): `InstanceCreatedEvent` → Payment & Notification modules listen
- **APIs** (Sync): Instance module queries Payment for plan limits
- **No Direct Calls**: Services never autowired across modules

See [02-MODULE-COMMUNICATION.md](02-MODULE-COMMUNICATION.md) for patterns.

---

## 🔍 Finding Specific Topics

| Topic | Location |
|-------|----------|
| How to create a new API endpoint | [05-API-ENDPOINTS.md](05-API-ENDPOINTS.md) |
| How to call another module safely | [02-MODULE-COMMUNICATION.md](02-MODULE-COMMUNICATION.md) |
| How to handle errors correctly | [03-EXCEPTION-HANDLING.md](03-EXCEPTION-HANDLING.md) |
| How to validate request input | [04-DTO-AND-VALIDATION.md](04-DTO-AND-VALIDATION.md) |
| How to write tests | [06-TESTING.md](06-TESTING.md) |
| How to secure sensitive data | [07-SECURITY.md](07-SECURITY.md) |
| How to name classes and methods | [08-CODE-QUALITY.md](08-CODE-QUALITY.md) |
| How to prepare for Phase 2 | [09-PHASE-2-PREPARATION.md](09-PHASE-2-PREPARATION.md) |
| What the full module structure is | [01-ARCHITECTURE-OVERVIEW.md](01-ARCHITECTURE-OVERVIEW.md) |

---

## 🎓 Learning Path

**For New Team Members:**
1. Start with [01-ARCHITECTURE-OVERVIEW.md](01-ARCHITECTURE-OVERVIEW.md) - Understand the big picture
2. Read [02-MODULE-COMMUNICATION.md](02-MODULE-COMMUNICATION.md) - Understand how modules interact
3. Read [05-API-ENDPOINTS.md](05-API-ENDPOINTS.md) - Understand REST patterns
4. Read [06-TESTING.md](06-TESTING.md) - Understand testing approach
5. Review [07-SECURITY.md](07-SECURITY.md) - Understand security requirements
6. Check [08-CODE-QUALITY.md](08-CODE-QUALITY.md) - Understand code standards
7. Skim [09-PHASE-2-PREPARATION.md](09-PHASE-2-PREPARATION.md) - Understand future direction

**For Implementing Features:**
1. Reference [01-ARCHITECTURE-OVERVIEW.md](01-ARCHITECTURE-OVERVIEW.md) for module locations
2. Use [02-MODULE-COMMUNICATION.md](02-MODULE-COMMUNICATION.md) for dependencies
3. Follow [04-DTO-AND-VALIDATION.md](04-DTO-AND-VALIDATION.md) for DTOs
4. Implement with [05-API-ENDPOINTS.md](05-API-ENDPOINTS.md) patterns
5. Test with [06-TESTING.md](06-TESTING.md) guidelines
6. Secure with [07-SECURITY.md](07-SECURITY.md) checklist
7. Review with [08-CODE-QUALITY.md](08-CODE-QUALITY.md) standards

**For Code Reviews:**
1. Check [02-MODULE-COMMUNICATION.md](02-MODULE-COMMUNICATION.md) - Module boundaries respected
2. Check [03-EXCEPTION-HANDLING.md](03-EXCEPTION-HANDLING.md) - Errors handled correctly
3. Check [04-DTO-AND-VALIDATION.md](04-DTO-AND-VALIDATION.md) - Input validated, sensitive data protected
4. Check [05-API-ENDPOINTS.md](05-API-ENDPOINTS.md) - REST conventions followed
5. Check [06-TESTING.md](06-TESTING.md) - Tests present and meaningful
6. Check [07-SECURITY.md](07-SECURITY.md) - Security checklist passed
7. Check [08-CODE-QUALITY.md](08-CODE-QUALITY.md) - Quality standards met

---

## 💡 Core Principles

### 1. Module Isolation
- ✅ Each module owns its domain
- ✅ Services are internal to modules
- ❌ NO direct service-to-service imports

See [01-ARCHITECTURE-OVERVIEW.md](01-ARCHITECTURE-OVERVIEW.md)

### 2. Async Communication
- ✅ Events for notifications (loosely coupled)
- ✅ APIs for queries (tightly scoped)
- ❌ NO tight coupling between modules

See [02-MODULE-COMMUNICATION.md](02-MODULE-COMMUNICATION.md)

### 3. Consistent Errors
- ✅ All exceptions extend `ApiException`
- ✅ Global handler catches all errors
- ✅ Consistent response format

See [03-EXCEPTION-HANDLING.md](03-EXCEPTION-HANDLING.md)

### 4. Input Validation
- ✅ Validate at boundary with `@Valid`
- ✅ Use Bean Validation annotations
- ✅ Never expose sensitive data

See [04-DTO-AND-VALIDATION.md](04-DTO-AND-VALIDATION.md)

### 5. RESTful Design
- ✅ Versioned paths (`/api/v1/`)
- ✅ Correct HTTP methods
- ✅ Appropriate status codes

See [05-API-ENDPOINTS.md](05-API-ENDPOINTS.md)

### 6. Comprehensive Testing
- ✅ Unit tests for business logic
- ✅ Integration tests for module
- ✅ Contract tests for APIs
- ✅ 70%+ coverage target

See [06-TESTING.md](06-TESTING.md)

### 7. Security First
- ✅ Hash passwords (BCrypt)
- ✅ Use JWT tokens
- ✅ Never log sensitive data
- ✅ Verify webhook signatures

See [07-SECURITY.md](07-SECURITY.md)

### 8. Code Excellence
- ✅ Clear naming conventions
- ✅ Complete Javadoc
- ✅ Single responsibility
- ✅ DRY principle

See [08-CODE-QUALITY.md](08-CODE-QUALITY.md)

### 9. Future-Ready Design
- ✅ Module APIs (become Feign clients)
- ✅ Events (become Kafka topics)
- ✅ Idempotent operations
- ✅ Clear transaction boundaries

See [09-PHASE-2-PREPARATION.md](09-PHASE-2-PREPARATION.md)

---

## ❓ Frequently Asked Questions

**Q: How do I call another module's code?**  
A: Use the module's `*ModuleApi` interface, not direct service imports. See [02-MODULE-COMMUNICATION.md](02-MODULE-COMMUNICATION.md)

**Q: What exceptions should I throw?**  
A: Extend `ApiException` with a specific code and details. See [03-EXCEPTION-HANDLING.md](03-EXCEPTION-HANDLING.md)

**Q: How do I validate user input?**  
A: Use `@Valid` annotation on DTOs with Bean Validation constraints. See [04-DTO-AND-VALIDATION.md](04-DTO-AND-VALIDATION.md)

**Q: What HTTP status codes should I use?**  
A: 201 for creation, 400 for validation, 403 for authorization, 404 for not found. See [05-API-ENDPOINTS.md](05-API-ENDPOINTS.md)

**Q: How do I test my code?**  
A: Unit tests with Mockito, integration tests with @SpringBootTest. See [06-TESTING.md](06-TESTING.md)

**Q: How do I handle passwords securely?**  
A: Use BCrypt for hashing, never expose in responses. See [07-SECURITY.md](07-SECURITY.md)

**Q: What naming conventions should I follow?**  
A: PascalCase for classes, camelCase for methods/variables. See [08-CODE-QUALITY.md](08-CODE-QUALITY.md)

**Q: How do I prepare for Phase 2?**  
A: Use module APIs and events, design for idempotency, document transactions. See [09-PHASE-2-PREPARATION.md](09-PHASE-2-PREPARATION.md)

---

## 📞 Need Help?

1. **Check the relevant guide** - Use the index above to find your topic
2. **Search this directory** - Files are organized by concern (Architecture, Communication, etc.)
3. **Consult the code examples** - Each guide includes practical code samples
4. **Review the checklists** - Each guide ends with actionable checklist items

---

## 📚 Related Resources

- **Assignment Document:** `../crudcloud_assignment.md`
- **Database Schema:** `../../sql/schema.sql`
- **Architecture Decision Records:** `../../docs/adr/`
- **API Documentation:** Swagger at `http://localhost:8080/swagger-ui.html`

---

## 📊 Document Structure

```
.github/instructions/
├── _INDEX.md (this file)
├── 01-ARCHITECTURE-OVERVIEW.md (foundations)
├── 02-MODULE-COMMUNICATION.md (patterns)
├── 03-EXCEPTION-HANDLING.md (errors)
├── 04-DTO-AND-VALIDATION.md (contracts)
├── 05-API-ENDPOINTS.md (REST design)
├── 06-TESTING.md (quality)
├── 07-SECURITY.md (protection)
├── 08-CODE-QUALITY.md (standards)
└── 09-PHASE-2-PREPARATION.md (future)
```

Each file is self-contained but cross-referenced with others. Total: 9 focused documents covering all aspects.

---

**Last Updated:** November 2025  
**Version:** 1.0 (Phase 1 - MVP)  
**Total Content:** 9 focused guides (~150 pages equivalent)

---

## 🎯 Quick Start for New Developers

1. **Clone & Setup:** Follow project README
2. **Read:** Start with [01-ARCHITECTURE-OVERVIEW.md](01-ARCHITECTURE-OVERVIEW.md)
3. **Pick a Module:** Look at `src/main/java/com/riwi/crudcloud/{module}/`
4. **Follow Patterns:** Use the appropriate guide from this index
5. **Ask Questions:** Check the guide section or related FAQ

**First Task:** Implement a simple feature in the Auth or Catalog module to familiarize yourself with the patterns!

---

**Happy Coding! 🚀**
