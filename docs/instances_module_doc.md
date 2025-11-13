# CrudCloud Backend – Instances Module

## Overview

The **Instances** module is part of the **CrudCloud** platform backend, developed by the *Crudzaso* team.  
Its purpose is to **manage real database instances** deployed as **Docker containers** on the assigned VPS.

This module allows users to:
- Create, list, suspend, resume, and delete instances
- Download a PDF with credentials (shown only once)
- Rotate passwords securely
- Receive email notifications
- Query the catalog of available database engines

The backend is built with **Spring Boot**, following a layered architecture:

```
Controller → Service → Repository → Model / DTO
```

---

## Module Architecture

| Layer | Description | Main Classes |
|-------|-------------|--------------|
| **Controller** | Exposes REST endpoints to interact with instances | `InstanceController` |
| **Service** | Contains business logic: container creation, validations, PDF generation, email, etc. | `InstanceService` |
| **Repository** | Interface that manages database access using Spring Data JPA | `InstanceRepository` |
| **Model / DTO** | Defines entities and data transfer objects | `Instance`, `InstanceResponse`, `InstanceCreateRequest` |

---

## Available Endpoints

> **Base URL:** `/instances`

### 1. Create an Instance
**POST** `/instances`

Creates a new database instance in a Docker container.  
The system validates the plan limit (Free, Standard, or Premium).

#### Request Body
```json
{
  "userId": 1,
  "dbType": "MYSQL",
  "plan": "FREE"
}
```

#### Response 201 (Created)
```json
{
  "id": 101,
  "dbType": "MYSQL",
  "status": "CREATING",
  "host": "10.0.0.5",
  "port": 3306,
  "username": "user_101",
  "password": "generated_once"
}
```

### 2. List Instances by User
**GET** `/instances/user/{userId}`

Retrieves all active instances for a specific user.
(In a real environment, the userId is obtained from the JWT token).

#### Response 200 (OK)
```json
[
  {
    "id": 101,
    "dbType": "MYSQL",
    "status": "RUNNING"
  },
  {
    "id": 102,
    "dbType": "POSTGRESQL",
    "status": "SUSPENDED"
  }
]
```

### 3. Get Instance Details
**GET** `/instances/{instanceId}`

Returns the complete details of a specific instance.

#### Response 200 (OK)
```json
{
  "id": 101,
  "dbType": "MYSQL",
  "status": "RUNNING",
  "host": "10.0.0.5",
  "port": 3306
}
```

### 4. Suspend Instance
**PATCH** `/instances/{instanceId}/suspend`

Stops the container associated with the instance.
Changes its status to SUSPENDED.

#### Response 200 (OK)
```json
{
  "id": 101,
  "status": "SUSPENDED"
}
```

### 5. Resume Instance
**PATCH** `/instances/{instanceId}/resume`

Restarts the container of the suspended instance.

#### Response 200 (OK)
```json
{
  "id": 101,
  "status": "RUNNING"
}
```

### 6. Rotate Password
**PATCH** `/instances/{instanceId}/rotate-password`

Generates a new password and replaces the previous one.
An email is sent with essential information (without the password in plain text).

#### Response 200 (OK)
```json
{
  "id": 101,
  "status": "RUNNING",
  "message": "Password rotated successfully"
}
```

### 7. Delete Instance
**DELETE** `/instances/{instanceId}`

Performs a soft delete of the instance and removes its container from the VPS.

#### Response 204 (No Content)
No content.

### 8. Download Credentials (PDF)
**GET** `/instances/{instanceId}/credentials-pdf`

Downloads a PDF with connection credentials (host, port, username, and password).
The document can only be downloaded once.

#### Response 200 (OK)
Content type: `application/pdf`

Header:
```
Content-Disposition: attachment; filename="crudcloud_credentials_<instanceId>.pdf"
```

### 9. Database Engine Catalog
**GET** `/instances/catalog`

Returns the list of compatible database engines.

#### Response 200 (OK)
```json
[
  "MYSQL",
  "SQLSERVER",
  "POSTGRESQL",
  "REDIS",
  "CASSANDRA",
  "MONGODB"
]
```

---

## Security and Credential Management

- Passwords are randomly generated and not stored in plain text
- Only shown once (when creating the instance or downloading the PDF)
- On each rotation, the previous password is invalidated
- Notification emails are sent without including the visible password

---

## Main Dependencies

| Dependency | Usage |
|------------|-------|
| Spring Boot Starter Web | REST controllers and HTTP request handling |
| Spring Boot Starter Validation (Jakarta) | Input validation with annotations (@Valid, @NotNull, etc.) |
| Spring Data JPA / Hibernate | Entity persistence |
| Spring Boot Starter Mail | Email notification sending |
| PDF Library (iTextPDF / OpenPDF) | PDF document generation with credentials |
| Lombok | Boilerplate reduction with annotations (@RequiredArgsConstructor, @Getter, etc.) |

---

## Local Execution

### Prerequisites
- Java 21+
- Maven 3.9+
- Docker (for real instances)
- Environment variables configured for email and database

### Commands
```bash
# Compile the project
mvn clean install

# Run locally
mvn spring-boot:run
```

The backend will be available at:
```
http://localhost:8080
```

---

## Module Owner

**Name:** Yeferson Alejandro Garcia Marin

**Role:** Backend Developer – Instances Module  

**Project:** CrudActivity – CrudCloud (Crudzaso Team)

---

© 2025 Crudzaso Team – CrudActivity Academic Project.  
All rights reserved.