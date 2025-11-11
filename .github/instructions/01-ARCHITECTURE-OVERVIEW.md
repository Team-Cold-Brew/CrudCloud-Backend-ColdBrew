# 🏗️ CrudCloud Architecture Overview

## 📋 Project Overview

**CrudCloud** is a cloud-based SaaS platform for provisioning and managing database instances via Docker containers. The project follows a **strategic two-phase architectural approach**:

- **Phase 1 (MVP):** Modular Monolith with event-driven communication
- **Phase 2 (Post-MVP):** Full microservices architecture

These instructions guide implementation to maintain architectural coherence, ensure Phase 1 → Phase 2 readiness, and enforce code quality standards throughout development.

---

## 🎯 Core Architectural Principles (PHASE 1 MVP)

### Module Structure & Separation of Concerns

The application is organized into **domain modules**, each with complete vertical slice ownership:

```
src/main/java/com/riwi/crudcloud/
├── core/                    [Shared Infrastructure]
│   ├── config/             [Global Spring configuration]
│   ├── exception/          [Global exception handling]
│   ├── event/              [Domain events for cross-module communication]
│   ├── util/               [Shared utilities]
│   └── constant/           [Application constants]
│
├── auth/                    [Authentication & Authorization Module]
│   ├── config/             [JWT, security config]
│   ├── controller/         [@RequestMapping("/api/auth")]
│   ├── service/            [Business logic: AuthService, UserService, TokenService]
│   ├── repository/         [UserRepository, OrganizationRepository]
│   ├── model/              [User, Organization, UserType]
│   ├── dto/                [Request/Response DTOs]
│   ├── exception/          [Auth-specific exceptions]
│   ├── util/               [JwtUtil, PasswordUtil]
│   └── api/                [AuthModuleApi interface - public contract]
│
├── instance/               [Instance Management Module]
│   ├── config/             [Docker config, instance-specific settings]
│   ├── controller/         [@RequestMapping("/api/instances")]
│   ├── service/            [InstanceService, DockerOrchestrationService, CredentialService]
│   ├── repository/         [InstanceRepository, InstanceStatisticsRepository]
│   ├── model/              [Instance, InstanceStatus, DatabaseEngine]
│   ├── dto/                [CreateInstanceRequest, InstanceResponse, etc.]
│   ├── exception/          [InstanceCreationException, DockerOperationException]
│   ├── util/               [InstanceUtil, PortAllocationUtil]
│   └── api/                [InstanceModuleApi interface - public contract]
│
├── payment/                [Payment & Transaction Module]
│   ├── config/             [MercadoPago config, payment settings]
│   ├── controller/         [@RequestMapping("/api/payments")]
│   ├── service/            [PaymentService, MercadoPagoService, TransactionService]
│   ├── repository/         [TransactionRepository]
│   ├── model/              [Transaction, TransactionStatus]
│   ├── dto/                [CreatePreferenceRequest, PaymentResponse, WebhookPayload]
│   ├── exception/          [PaymentProcessingException]
│   ├── util/               [PaymentUtil, WebhookVerificationUtil]
│   └── api/                [PaymentModuleApi interface - public contract]
│
├── plan/                   [Plan & Subscription Module]
│   ├── controller/         [@RequestMapping("/api/plans")]
│   ├── service/            [PlanService, PlanValidationService]
│   ├── repository/         [PlanRepository]
│   ├── model/              [Plan, PlanType]
│   ├── dto/                [PlanResponse, PlanLimitsResponse]
│   ├── exception/          [PlanLimitExceededException]
│   └── api/                [PlanModuleApi interface - public contract]
│
└── catalog/                [Database Engine Catalog Module]
    ├── controller/         [@RequestMapping("/api/engines")]
    ├── service/            [CatalogService]
    ├── model/              [DatabaseEngineInfo]
    └── dto/                [EngineResponse]
```

### Key Rules

- ✅ Each module owns its domain (models, DTOs, exceptions, repository)
- ✅ Services are internal to modules (never autowired across modules)
- ✅ Public contracts defined via `*ModuleApi` interfaces in the `api/` subfolder
- ❌ NO direct service-to-service dependencies across modules
- ❌ NO importing concrete service classes from other modules
- ✅ Cross-module communication via **Spring Events** or **module APIs** only

---

## 🔗 Related Documentation

See other guides for specific patterns:
- **Module Communication:** `02-MODULE-COMMUNICATION.md`
- **Exception Handling:** `03-EXCEPTION-HANDLING.md`
- **DTO Strategy:** `04-DTO-AND-VALIDATION.md`
- **API Endpoints:** `05-API-ENDPOINTS.md`
- **Testing Guidelines:** `06-TESTING.md`
- **Security Best Practices:** `07-SECURITY.md`
- **Code Quality:** `08-CODE-QUALITY.md`
- **Phase 2 Preparation:** `09-PHASE-2-PREPARATION.md`

---

**Last Updated:** November 2025  
**Version:** 1.0 (Phase 1 - MVP)
