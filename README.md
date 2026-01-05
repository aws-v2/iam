# IAM Service

> A production-ready Identity and Access Management (IAM) service built with Spring Boot 4 and Java 17, featuring JWT authentication, access key management, policy-based authorization, and distributed validation via NATS messaging.

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue.svg)](https://www.postgresql.org/)
[![NATS](https://img.shields.io/badge/NATS-2.24.1-blueviolet.svg)](https://nats.io/)

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Quick Start](#quick-start)
- [Configuration](#configuration)
  - [Application Properties](#application-properties)
  - [Environment Variables](#environment-variables)
  - [Docker Setup](#docker-setup)
- [API Documentation](#api-documentation)
  - [Authentication Endpoints](#authentication-endpoints)
  - [Access Key Endpoints](#access-key-endpoints)
  - [Policy Endpoints](#policy-endpoints)
  - [Health & Monitoring](#health--monitoring)
- [Security](#security)
  - [Authentication Flow](#authentication-flow)
  - [JWT Configuration](#jwt-configuration)
  - [Password Security](#password-security)
  - [Access Control](#access-control)
  - [CORS](#cors-configuration)
  - [Input Validation](#input-validation)
- [NATS Integration](#nats-integration)
- [Database Schema](#database-schema)
- [Code Structure](#code-structure)
- [Development](#development)
- [Deployment](#deployment)
- [Usage Examples](#usage-examples)
- [Error Handling](#error-handling)
- [Logging](#logging)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Performance & Scalability](#performance--scalability)
- [Security Best Practices](#security-best-practices)
- [Monitoring & Observability](#monitoring--observability)
- [Contributing](#contributing)
- [License](#license)
- [Roadmap](#roadmap)

---

## Overview

The **IAM Service** is a centralized Identity and Access Management system designed for microservices architectures. It provides secure user authentication, programmatic access key management, and policy-based authorization, similar to AWS IAM but designed for custom cloud platforms.

### What It Does

- **User Authentication**: Register and login with email/password, receive JWT tokens
- **Access Key Management**: Generate cryptographically secure access keys for programmatic access
- **Policy-Based Authorization**: Define and manage fine-grained access policies
- **Distributed Validation**: Validate access keys across services using NATS messaging
- **Audit & Monitoring**: Comprehensive logging and health monitoring

---

## Features

### Core Capabilities

✅ **JWT-Based Authentication**
- Secure registration and login
- HS256-signed tokens
- 24-hour token expiration
- BCrypt password hashing

✅ **Access Key Management**
- Generate AWS-style access keys (AKIA...)
- 40-character secret keys
- Status management (ACTIVE/INACTIVE)
- Ownership-based access control

✅ **Policy Management**
- JSON-based policy documents
- User-scoped policies
- Full CRUD operations
- Flexible authorization rules

✅ **NATS Integration**
- Request-reply validation pattern
- Topic: `iam.auth.validate`
- Secret key verification
- Policy retrieval

✅ **Production-Ready**
- Input validation
- Global exception handling
- Transaction management
- CORS configuration
- Health checks (Actuator)
- Environment variable support

---

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Client Apps                           │
│  (Web, Mobile, CLI, Other Services)                         │
└──────────────┬──────────────────────────────┬───────────────┘
               │                              │
               │ HTTP REST                    │ NATS
               │                              │
┌──────────────▼──────────────┐   ┌───────────▼──────────────┐
│   IAM Service (Spring Boot)  │   │   Service A, B, C...     │
│                              │   │   (Key Validation)       │
│  ┌────────────────────────┐ │   └───────────▲──────────────┘
│  │  Web Layer             │ │               │
│  │  - AuthController      │ │               │ Request-Reply
│  │  - AccessKeyController │ │               │
│  │  - PolicyController    │ │   ┌───────────┴──────────────┐
│  └───────┬────────────────┘ │   │   NATS Message Broker    │
│          │                   │   │   iam.auth.validate      │
│  ┌───────▼────────────────┐ │   └──────────────────────────┘
│  │  Service Layer         │ │
│  │  - AuthService         │ │
│  │  - AccessKeyService    │ │
│  │  - PolicyService       │ │
│  └───────┬────────────────┘ │
│          │                   │
│  ┌───────▼────────────────┐ │
│  │  Repository Layer      │ │
│  │  - UserRepository      │ │
│  │  - AccessKeyRepository │ │
│  │  - PolicyRepository    │ │
│  └───────┬────────────────┘ │
└──────────┼───────────────────┘
           │
┌──────────▼───────────────────┐
│   PostgreSQL Database        │
│   - users                    │
│   - access_keys              │
│   - policies                 │
└──────────────────────────────┘
```

### Layered Architecture

The application follows the **Controller-Service-Repository** pattern:

1. **Web Layer** (`web/`): REST controllers handling HTTP requests
2. **Service Layer** (`service/`): Business logic and orchestration
3. **Repository Layer** (`repository/`): Data access via Spring Data JPA
4. **Configuration Layer** (`config/`): Security, JWT, NATS, Jackson setup
5. **Messaging Layer** (`messaging/`): NATS listener for async validation

---

## Technology Stack

### Backend Framework
- **Java**: 17 (LTS)
- **Spring Boot**: 4.0.1
- **Spring Security**: JWT authentication, BCrypt hashing
- **Spring Data JPA**: Database access with Hibernate

### Database
- **PostgreSQL**: 17 (Docker)
- **Hibernate**: Auto-schema management

### Messaging
- **NATS**: 2.24.1 (Request-reply pattern)

### Security
- **JJWT**: 0.12.6 (JWT generation/validation)
- **BCrypt**: Password hashing

### Utilities
- **Lombok**: Reduce boilerplate
- **Jackson**: JSON serialization
- **Spring Boot Actuator**: Health checks
- **Jakarta Bean Validation**: Input validation

### Build & Runtime
- **Maven**: 3.6+ (Build tool)
- **Docker**: Containerization
- **Docker Compose**: Multi-container orchestration

---

## Getting Started

### Prerequisites

Before you begin, ensure you have the following installed:

- **Java 17 or higher** - [Download](https://adoptium.net/)
- **Docker** and **Docker Compose** - [Download](https://www.docker.com/)
- **Maven 3.6+** (optional, wrapper included)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd IAM
   ```

2. **Build the project**
   ```bash
   ./mvnw clean install
   ```

3. **Start dependencies**
   ```bash
   docker-compose up -d
   ```
   This starts PostgreSQL (port 5432) and NATS (port 4222, Web UI: 8222)

4. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```
   The service will start on **http://localhost:8081**

### Quick Start

Follow these steps to verify everything works:

#### 1. Check Health
```bash
curl http://localhost:8081/actuator/health
```

Expected response:
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"}
  }
}
```

#### 2. Register a User
```bash
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "password": "SecurePass123"
  }'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1NTBlODQwMC1lMjliLTQxZDQtYTcxNi00NDY2NTU0NDAwMDAiLCJpYXQiOjE3MDQ5Nzg2MDAsImV4cCI6MTcwNTA2NTAwMH0...",
  "message": "User registered successfully"
}
```

#### 3. Create an Access Key
```bash
TOKEN="<your-token-from-registration>"

curl -X POST http://localhost:8081/api/keys \
  -H "Authorization: Bearer $TOKEN"
```

Response (secret shown only once!):
```json
{
  "accessKeyId": "AKIA1234567890123456",
  "secretAccessKey": "abcd1234efgh5678ijkl9012mnop3456qrst",
  "status": "ACTIVE"
}
```

#### 4. Create a Policy
```bash
curl -X POST http://localhost:8081/api/policies \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "ReadOnlyPolicy",
    "policyDocument": "{\"Version\": \"2012-10-17\", \"Statement\": [{\"Effect\": \"Allow\", \"Action\": [\"s3:GetObject\"], \"Resource\": \"*\"}]}"
  }'
```

#### 5. Test NATS Validation
Using NATS CLI:
```bash
nats req iam.auth.validate '{
  "accessKeyId": "AKIA1234567890123456",
  "secretAccessKey": "abcd1234efgh5678ijkl9012mnop3456qrst"
}'
```

Response:
```json
{
  "valid": true,
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "policies": ["{\"Version\": \"2012-10-17\", ...}"]
}
```

---

## Configuration

### Application Properties

The service is configured via `src/main/resources/application.properties`:

```properties
# Server Configuration
server.port=8081

# Database (PostgreSQL)
spring.datasource.url=jdbc:postgresql://localhost:5432/iam_db
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# NATS Messaging
nats.url=nats://localhost:4222

# JWT Security
jwt.secret=5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437
jwt.expiration=86400000

# Actuator (Health Monitoring)
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always
```

### Environment Variables

All configuration can be overridden with environment variables:

| Variable | Description | Default Value | Required |
|----------|-------------|---------------|----------|
| `SERVER_PORT` | Application port | `8081` | No |
| `DATABASE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/iam_db` | No |
| `DATABASE_USERNAME` | Database username | `root` | No |
| `DATABASE_PASSWORD` | Database password | `root` | No |
| `NATS_URL` | NATS server URL | `nats://localhost:4222` | No |
| `JWT_SECRET` | JWT signing secret (hex) | *default-hex-value* | **Yes** (production) |
| `JWT_EXPIRATION` | Token lifetime (ms) | `86400000` (24h) | No |
| `JPA_DDL_AUTO` | Hibernate schema mode | `update` | No |
| `JPA_SHOW_SQL` | Log SQL statements | `true` | No |

**Production Example:**
```bash
export JWT_SECRET="your-256-bit-hex-secret-here"
export DATABASE_URL="jdbc:postgresql://prod-db:5432/iam"
export DATABASE_USERNAME="iam_prod_user"
export DATABASE_PASSWORD="strong-password"
./mvnw spring-boot:run
```

### Docker Setup

The `docker-compose.yml` defines two services:

```yaml
services:
  postgres:
    image: postgres:17
    container_name: sacco_postgres
    environment:
      POSTGRES_DB: iam_db
      POSTGRES_USER: root
      POSTGRES_PASSWORD: root
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  nats:
    image: nats:latest
    container_name: iam_nats
    ports:
      - "4222:4222"  # NATS client
      - "8222:8222"  # Web UI

volumes:
  postgres_data:
```

**Commands:**
- Start: `docker-compose up -d`
- Stop: `docker-compose down`
- Logs: `docker-compose logs -f`
- NATS UI: http://localhost:8222

---

## API Documentation

### Authentication Endpoints

#### POST /auth/register

Register a new user account.

**Request:**
```bash
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "MySecure123"
  }'
```

**Request Body:**
```json
{
  "email": "string (valid email format)",
  "password": "string (min 8 characters)"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "message": "User registered successfully"
}
```

**Errors:**
- `400 Bad Request`: Invalid email format or password too short
- `400 Bad Request`: Email already in use

---

#### POST /auth/login

Authenticate and receive JWT token.

**Request:**
```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "MySecure123"
  }'
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyLWlkIiwiaWF0IjoxNjcwMDAwMDAwLCJleHAiOjE2NzAwODY0MDB9...",
  "message": "Login successful"
}
```

**Errors:**
- `400 Bad Request`: Invalid credentials

---

### Access Key Endpoints

All access key endpoints require authentication via `Authorization: Bearer <token>` header.

#### GET /api/keys

List user's access keys (secrets hidden).

**Request:**
```bash
curl http://localhost:8081/api/keys \
  -H "Authorization: Bearer $TOKEN"
```

**Response (200 OK):**
```json
[
  {
    "accessKeyId": "AKIA1234567890123456",
    "status": "ACTIVE",
    "createdAt": "2024-01-04T10:30:45.123456"
  },
  {
    "accessKeyId": "AKIA9876543210987654",
    "status": "INACTIVE",
    "createdAt": "2024-01-03T15:20:10.654321"
  }
]
```

---

#### POST /api/keys

Create a new access key pair.

**Request:**
```bash
curl -X POST http://localhost:8081/api/keys \
  -H "Authorization: Bearer $TOKEN"
```

**Response (200 OK):**
```json
{
  "accessKeyId": "AKIA1234567890123456",
  "secretAccessKey": "abcd1234efgh5678ijkl9012mnop3456qrst",
  "status": "ACTIVE"
}
```

> **Warning**: The `secretAccessKey` is shown **only once**. Store it securely!

---

#### DELETE /api/keys/{accessKeyId}

Delete an access key permanently.

**Request:**
```bash
curl -X DELETE http://localhost:8081/api/keys/AKIA1234567890123456 \
  -H "Authorization: Bearer $TOKEN"
```

**Response (204 No Content)**

**Errors:**
- `400 Bad Request`: Access key not found
- `400 Bad Request`: Unauthorized (not your key)

---

#### PUT /api/keys/{accessKeyId}/status

Update access key status (activate/deactivate).

**Request:**
```bash
curl -X PUT http://localhost:8081/api/keys/AKIA1234567890123456/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "INACTIVE"}'
```

**Request Body:**
```json
{
  "status": "ACTIVE" | "INACTIVE"
}
```

**Response (200 OK)**

---

### Policy Endpoints

#### POST /api/policies

Create a new policy.

**Request:**
```bash
curl -X POST http://localhost:8081/api/policies \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "S3ReadOnly",
    "policyDocument": "{\"Version\": \"2012-10-17\", \"Statement\": [{\"Effect\": \"Allow\", \"Action\": [\"s3:GetObject\"], \"Resource\": \"*\"}]}"
  }'
```

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "S3ReadOnly",
  "policyDocument": "{\"Version\": \"2012-10-17\", ...}"
}
```

---

#### GET /api/policies

List all user's policies.

**Request:**
```bash
curl http://localhost:8081/api/policies \
  -H "Authorization: Bearer $TOKEN"
```

**Response (200 OK):**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "S3ReadOnly",
    "policyDocument": "{...}"
  }
]
```

---

#### GET /api/policies/{policyId}

Get a specific policy by ID.

**Request:**
```bash
curl http://localhost:8081/api/policies/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer $TOKEN"
```

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "S3ReadOnly",
  "policyDocument": "{...}"
}
```

---

#### DELETE /api/policies/{policyId}

Delete a policy.

**Request:**
```bash
curl -X DELETE http://localhost:8081/api/policies/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer $TOKEN"
```

**Response (204 No Content)**

---

### Health & Monitoring

#### GET /actuator/health

Service health status (no authentication required).

**Request:**
```bash
curl http://localhost:8081/actuator/health
```

**Response (200 OK):**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500000000000,
        "free": 250000000000
      }
    }
  }
}
```

---

#### GET /actuator/info

Application metadata.

**Request:**
```bash
curl http://localhost:8081/actuator/info
```

---

## Security

### Authentication Flow

#### Registration Flow
```
Client                    IAM Service                Database
  |                           |                         |
  | POST /auth/register       |                         |
  |-------------------------->|                         |
  |  {email, password}        |                         |
  |                           | Check email exists      |
  |                           |------------------------>|
  |                           |<------------------------|
  |                           | BCrypt hash password    |
  |                           | Save user               |
  |                           |------------------------>|
  |                           |<------------------------|
  |                           | Generate JWT (HS256)    |
  |<--------------------------|                         |
  | {token, message}          |                         |
```

#### Login Flow
```
Client                    IAM Service                Database
  |                           |                         |
  | POST /auth/login          |                         |
  |-------------------------->|                         |
  |  {email, password}        |                         |
  |                           | Find user by email      |
  |                           |------------------------>|
  |                           |<------------------------|
  |                           | Verify password (BCrypt)|
  |                           | Generate JWT            |
  |<--------------------------|                         |
  | {token, message}          |                         |
```

#### Protected Endpoint Flow
```
Client                    Filter                  Controller
  |                           |                         |
  | GET /api/keys             |                         |
  | Authorization: Bearer ... |                         |
  |-------------------------->|                         |
  |                           | Extract token           |
  |                           | Validate JWT            |
  |                           | Set userId in context   |
  |                           |------------------------>|
  |                           |                         |
  |<--------------------------------------------------|
  | {data}                    |                         |
```

### JWT Configuration

**Algorithm**: HS256 (HMAC with SHA-256)

**Token Structure**:
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
.
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "iat": 1704978600,
  "exp": 1705065000
}
.
<signature>
```

**Claims:**
- `sub`: User UUID
- `iat`: Issued at timestamp
- `exp`: Expiration timestamp (24 hours)

**Secret Key Management:**
- Must be 256-bit (32 bytes) for HS256
- Stored as hex string in configuration
- Override with `JWT_SECRET` env var in production

### Password Security

- **Algorithm**: BCrypt
- **Salt Rounds**: Default (10+)
- **Implementation**: Spring Security `BCryptPasswordEncoder`
- **Constant-Time Comparison**: Prevents timing attacks

### Access Control

**Authorization Rules:**

| Endpoint Pattern | Authentication | Authorization |
|-----------------|----------------|---------------|
| `/auth/**` | None | Public |
| `/actuator/**` | None | Public |
| `/api/keys/**` | JWT Required | Owner only |
| `/api/policies/**` | JWT Required | Owner only |

**Ownership Validation:**
- Services verify `userId` from JWT matches resource owner
- Returns `400 Bad Request` if unauthorized

### CORS Configuration

**Allowed Origins:**
- `http://localhost:3000` (React dev server)
- `http://localhost:8080` (Vue/Angular dev server)

**Allowed Methods:**
- GET, POST, PUT, DELETE, OPTIONS

**Allowed Headers:** All (`*`)

**Credentials:** Allowed

**Max Age:** 3600 seconds (1 hour)

### Input Validation

**Validation Annotations:**
- `@NotBlank`: Field cannot be null or empty
- `@Email`: Valid email format
- `@Size(min=8)`: Minimum password length

**Example Errors:**
```json
{
  "error": "Validation Error",
  "message": "Email is required, Password must be at least 8 characters",
  "timestamp": "2024-01-04T10:30:45.123456"
}
```

---

## NATS Integration

### Overview

The IAM service acts as a **validation authority** for distributed systems. Other microservices (S3, EC2, etc.) can validate access keys and retrieve user policies via NATS messaging.

### Architecture

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│ S3 Service  │         │  NATS Broker │         │ IAM Service │
│             │         │              │         │             │
│ 1. Request  │-------->│              │-------->│ 3. Validate │
│             │         │              │         │    Key +    │
│             │         │              │         │    Secret   │
│             │         │              │         │             │
│ 4. Response │<--------|              │<--------|             │
│             │         │              │         │             │
└─────────────┘         └──────────────┘         └─────────────┘
```

### Validation Protocol

**Topic:** `iam.auth.validate`

**Request Format:**
```json
{
  "accessKeyId": "AKIA1234567890123456",
  "secretAccessKey": "abcd1234efgh5678ijkl9012mnop3456qrst"
}
```

**Response Format (Valid):**
```json
{
  "valid": true,
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "policies": [
    "{\"Version\": \"2012-10-17\", \"Statement\": [...]}",
    "{\"Version\": \"2012-10-17\", \"Statement\": [...]}"
  ]
}
```

**Response Format (Invalid):**
```json
{
  "valid": false,
  "userId": null,
  "policies": null
}
```

### Validation Logic

1. Check if `accessKeyId` exists in database
2. Verify `status` is `ACTIVE`
3. Compare `secretAccessKey` (constant-time)
4. Retrieve all user's policies
5. Return validation result

### Usage Example (Java with JNats)

```java
import io.nats.client.*;
import com.fasterxml.jackson.databind.ObjectMapper;

Connection nc = Nats.connect("nats://localhost:4222");
ObjectMapper mapper = new ObjectMapper();

// Build request
Map<String, String> request = Map.of(
    "accessKeyId", "AKIA1234567890123456",
    "secretAccessKey", "abcd1234efgh5678ijkl9012mnop3456qrst"
);

// Send request-reply
Message reply = nc.request("iam.auth.validate",
    mapper.writeValueAsBytes(request), Duration.ofSeconds(5));

// Parse response
ValidationResponse response = mapper.readValue(reply.getData(), ValidationResponse.class);

if (response.isValid()) {
    System.out.println("User ID: " + response.getUserId());
    System.out.println("Policies: " + response.getPolicies());
} else {
    System.out.println("Invalid credentials");
}
```

### Testing with NATS CLI

```bash
# Install NATS CLI
go install github.com/nats-io/natscli/nats@latest

# Test validation
nats req iam.auth.validate '{
  "accessKeyId": "AKIA1234567890123456",
  "secretAccessKey": "abcd1234efgh5678ijkl9012mnop3456qrst"
}'
```

---

## Database Schema

### Entity Relationship Diagram

```
┌─────────────────┐
│     users       │
├─────────────────┤
│ id (UUID) PK    │
│ email (UNIQUE)  │
│ password_hash   │
│ created_at      │
└────────┬────────┘
         │
         │ 1:M
         │
    ┌────┴──────────────────┐
    │                       │
┌───▼──────────────┐  ┌────▼─────────────┐
│  access_keys     │  │    policies      │
├──────────────────┤  ├──────────────────┤
│ access_key_id PK │  │ id (UUID) PK     │
│ secret_key       │  │ name             │
│ status (ENUM)    │  │ policy_document  │
│ created_at       │  │ user_id (FK)     │
│ user_id (FK)     │  └──────────────────┘
└──────────────────┘
```

### Table: `users`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PRIMARY KEY | User unique identifier |
| `email` | VARCHAR | UNIQUE, NOT NULL | User email address |
| `password_hash` | VARCHAR | NOT NULL | BCrypt hashed password |
| `created_at` | TIMESTAMP | NOT NULL | Account creation time |

**SQL:**
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
```

---

### Table: `access_keys`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `access_key_id` | VARCHAR(20) | PRIMARY KEY | Public access key (AKIA...) |
| `secret_access_key` | VARCHAR(40) | NOT NULL | Secret key (40 chars) |
| `status` | VARCHAR | NOT NULL | ACTIVE or INACTIVE |
| `created_at` | TIMESTAMP | NOT NULL | Key creation time |
| `user_id` | UUID | FOREIGN KEY | References users(id) |

**SQL:**
```sql
CREATE TABLE access_keys (
    access_key_id VARCHAR(20) PRIMARY KEY,
    secret_access_key VARCHAR(40) NOT NULL,
    status VARCHAR(10) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id UUID NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_access_keys_user_id ON access_keys(user_id);
```

---

### Table: `policies`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PRIMARY KEY | Policy unique identifier |
| `name` | VARCHAR | NOT NULL | Policy name |
| `policy_document` | TEXT | NOT NULL | JSON policy document |
| `user_id` | UUID | FOREIGN KEY | References users(id) |

**SQL:**
```sql
CREATE TABLE policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    policy_document TEXT NOT NULL,
    user_id UUID NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_policies_user_id ON policies(user_id);
```

---

## Code Structure

### Package Organization

```
src/main/java/org/serwin/iam/
├── IamApplication.java          # Spring Boot entry point
├── config/                      # Configuration beans
│   ├── JacksonConfig.java       # JSON serialization config
│   ├── JwtAuthenticationFilter.java  # JWT filter
│   ├── NatsConfig.java          # NATS connection bean
│   └── SecurityConfig.java      # Security & CORS
├── domain/                      # JPA entities
│   ├── AccessKey.java           # Access key entity
│   ├── Policy.java              # Policy entity
│   └── User.java                # User entity
├── dto/                         # Data Transfer Objects
│   └── DTOs.java                # All request/response DTOs
├── exception/                   # Error handling
│   ├── ErrorResponseDTO.java    # Error response format
│   └── GlobalExceptionHandler.java  # @ControllerAdvice
├── messaging/                   # NATS integration
│   └── AuthNatsListener.java    # Validation listener
├── repository/                  # Spring Data JPA
│   ├── AccessKeyRepository.java
│   ├── PolicyRepository.java
│   └── UserRepository.java
├── service/                     # Business logic
│   ├── AccessKeyService.java    # Key management
│   ├── AuthService.java         # Authentication
│   └── PolicyService.java       # Policy management
├── util/                        # Utilities
│   └── JwtUtil.java             # JWT generation/validation
└── web/                         # REST controllers
    ├── AccessKeyController.java
    ├── AuthController.java
    └── PolicyController.java
```

### Key Components

**Domain Models:**
- `User`: User account entity
- `AccessKey`: Programmatic access credentials
- `Policy`: Authorization policies

**DTOs:**
- Request: `RegisterRequestDTO`, `LoginRequestDTO`, `CreatePolicyDTO`, etc.
- Response: `AuthResponseDTO`, `AccessKeyResponseDTO`, `PolicyResponseDTO`, etc.
- Validation: `ValidationRequestDTO`, `ValidationResponse`

**Repositories:**
- Spring Data JPA interfaces with custom query methods
- Example: `Optional<User> findByEmail(String email)`

**Services:**
- Business logic, validation, transactions
- `@Transactional` for data consistency

**Controllers:**
- REST endpoints, input validation (`@Valid`)
- Authentication via `@AuthenticationPrincipal UUID userId`

**Configuration:**
- `SecurityConfig`: JWT filter chain, CORS, CSRF
- `JwtUtil`: Token generation/validation
- `NatsConfig`: NATS connection factory

---

## Development

### Building the Project

```bash
# Clean and compile
./mvnw clean compile

# Run tests
./mvnw test

# Package JAR
./mvnw package

# Install to local Maven repo
./mvnw clean install
```

### Running Locally

1. **Start infrastructure**
   ```bash
   docker-compose up -d
   ```

2. **Run application**
   ```bash
   ./mvnw spring-boot:run
   ```

3. **Verify**
   ```bash
   curl http://localhost:8081/actuator/health
   ```

### Hot Reload (Optional)

Add Spring Boot DevTools to `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <optional>true</optional>
</dependency>
```

### IDE Setup

**IntelliJ IDEA:**
1. Open project as Maven project
2. Enable annotation processing (Lombok)
3. Set SDK to Java 17
4. Run `IamApplication.java`

**VS Code:**
1. Install Java Extension Pack
2. Install Spring Boot Extension Pack
3. Run: `Spring Boot Dashboard` > Start

---

## Deployment

### Docker Build

```bash
# Build Docker image
docker build -t iam-service:1.0.0 .

# Run container
docker run -d \
  -p 8081:8081 \
  -e JWT_SECRET="your-secret" \
  -e DATABASE_URL="jdbc:postgresql://host:5432/iam" \
  -e DATABASE_USERNAME="user" \
  -e DATABASE_PASSWORD="pass" \
  --name iam-service \
  iam-service:1.0.0
```

### Production Checklist

Before deploying to production:

- [ ] **Change JWT Secret** - Generate new 256-bit secret
- [ ] **Use Environment Variables** - Never commit secrets
- [ ] **Enable HTTPS** - Configure TLS termination (load balancer or reverse proxy)
- [ ] **Update CORS Origins** - Set to actual frontend domains
- [ ] **Database Backups** - Configure automated backups
- [ ] **Change Hibernate DDL Mode** - Set `spring.jpa.hibernate.ddl-auto=validate`
- [ ] **Enable Audit Logging** - Log all security events
- [ ] **Set Strong DB Password** - Use secret manager
- [ ] **Monitor Logs** - Set up log aggregation (ELK, Splunk)
- [ ] **Health Checks** - Configure liveness/readiness probes
- [ ] **Rate Limiting** - Add API rate limiting (future)
- [ ] **Rotate Access Keys** - Implement key rotation policy

### Kubernetes Deployment

**ConfigMap:**
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: iam-config
data:
  DATABASE_URL: "jdbc:postgresql://postgres-service:5432/iam"
  NATS_URL: "nats://nats-service:4222"
```

**Secret:**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: iam-secrets
type: Opaque
data:
  JWT_SECRET: <base64-encoded-secret>
  DATABASE_USERNAME: <base64-encoded-username>
  DATABASE_PASSWORD: <base64-encoded-password>
```

**Deployment:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: iam-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: iam
  template:
    metadata:
      labels:
        app: iam
    spec:
      containers:
      - name: iam
        image: iam-service:1.0.0
        ports:
        - containerPort: 8081
        env:
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: iam-secrets
              key: JWT_SECRET
        - name: DATABASE_URL
          valueFrom:
            configMapKeyRef:
              name: iam-config
              key: DATABASE_URL
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 10
          periodSeconds: 5
```

---

## Usage Examples

### Complete User Flow

```bash
# 1. Register
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"dev@example.com","password":"DevPass123"}'

# Save token
TOKEN="<token-from-response>"

# 2. Create Access Key
curl -X POST http://localhost:8081/api/keys \
  -H "Authorization: Bearer $TOKEN"

# Save credentials
ACCESS_KEY_ID="AKIA..."
SECRET_ACCESS_KEY="..."

# 3. Create Policy
curl -X POST http://localhost:8081/api/policies \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "S3FullAccess",
    "policyDocument": "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Action\":\"s3:*\",\"Resource\":\"*\"}]}"
  }'

# 4. List Keys
curl http://localhost:8081/api/keys \
  -H "Authorization: Bearer $TOKEN"

# 5. List Policies
curl http://localhost:8081/api/policies \
  -H "Authorization: Bearer $TOKEN"

# 6. Deactivate Key
curl -X PUT http://localhost:8081/api/keys/$ACCESS_KEY_ID/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"INACTIVE"}'

# 7. Delete Key
curl -X DELETE http://localhost:8081/api/keys/$ACCESS_KEY_ID \
  -H "Authorization: Bearer $TOKEN"
```

### Policy Document Examples

**Read-Only S3 Access:**
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:ListBucket"
      ],
      "Resource": "*"
    }
  ]
}
```

**Full S3 Access:**
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "s3:*",
      "Resource": "*"
    }
  ]
}
```

**Restricted S3 Access:**
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:GetObject", "s3:PutObject"],
      "Resource": "arn:aws:s3:::my-bucket/*"
    }
  ]
}
```

---

## Error Handling

### Error Response Format

All errors return a consistent JSON structure:

```json
{
  "error": "Bad Request",
  "message": "Email already in use",
  "timestamp": "2024-01-04T10:30:45.123456"
}
```

### HTTP Status Codes

| Code | Meaning | Common Scenarios |
|------|---------|------------------|
| `200 OK` | Success | Successful GET/POST/PUT |
| `204 No Content` | Success (no body) | Successful DELETE |
| `400 Bad Request` | Client error | Validation errors, invalid input |
| `401 Unauthorized` | Authentication failed | Invalid/expired JWT token |
| `500 Internal Server Error` | Server error | Unexpected exceptions |

### Common Errors

| Error Message | Cause | Solution |
|---------------|-------|----------|
| `Email already in use` | Duplicate registration | Use different email |
| `Invalid credentials` | Wrong email/password | Check credentials |
| `Invalid or expired token` | JWT expired or malformed | Login again |
| `Validation Error: Email is required` | Missing field | Provide all required fields |
| `Access key not found` | Wrong access key ID | Verify access key ID |
| `Unauthorized: You don't own this access key` | Not your resource | Use your own resources |

---

## Logging

### Log Levels

- **INFO**: Successful operations (registration, login, key creation)
- **WARN**: Failed attempts (invalid credentials, unauthorized access)
- **ERROR**: Exceptions and stack traces

### Key Log Events

**Authentication:**
```
INFO  - User registered: alice@example.com
INFO  - Login successful: alice@example.com
WARN  - Login failed: invalid credentials for alice@example.com
```

**Access Keys:**
```
INFO  - Access key created for user: 550e8400-e29b-41d4-a716-446655440000 (keyId: AKIA...)
INFO  - Access key deleted: keyId=AKIA..., userId=550e8400-e29b-41d4-a716-446655440000
INFO  - Access key status updated: keyId=AKIA..., newStatus=INACTIVE
```

**NATS Validation:**
```
INFO  - Key validation successful: keyId=AKIA..., userId=550e8400-e29b-41d4-a716-446655440000
WARN  - Key validation failed: Invalid or inactive access key - AKIA...
WARN  - Key validation failed: Secret key mismatch for access key - AKIA...
```

### Viewing Logs

**Spring Boot:**
```bash
# In terminal
./mvnw spring-boot:run

# Docker
docker logs -f iam-service

# Docker Compose
docker-compose logs -f postgres nats
```

---

## Testing

### Manual Testing with curl

See [Usage Examples](#usage-examples) section.

### Postman Collection

1. **Import collection** (create file `IAM.postman_collection.json`)
2. **Set variables:**
   - `base_url`: `http://localhost:8081`
   - `token`: `<JWT-from-login>`
3. **Run collection**

### NATS Testing

```bash
# Install NATS CLI
go install github.com/nats-io/natscli/nats@latest

# Publish test message
nats req iam.auth.validate '{
  "accessKeyId": "AKIA1234567890123456",
  "secretAccessKey": "abcd1234efgh5678ijkl9012mnop3456qrst"
}'

# Monitor NATS traffic
nats sub ">"
```

---

## Troubleshooting

### Common Issues

#### Database Connection Errors

**Symptom:**
```
ERROR: Connection to localhost:5432 refused
```

**Solution:**
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Restart Docker Compose
docker-compose down && docker-compose up -d

# Check logs
docker logs sacco_postgres
```

---

#### NATS Connection Errors

**Symptom:**
```
ERROR: Failed to connect to nats://localhost:4222
```

**Solution:**
```bash
# Check NATS status
docker ps | grep nats

# View NATS logs
docker logs iam_nats

# Test connectivity
nats pub test "hello"
```

---

#### JWT Validation Failures

**Symptom:**
```json
{"error":"Unauthorized","message":"Invalid or expired token"}
```

**Solution:**
- Token expired (24h lifetime) → Login again
- Wrong token format → Check `Authorization: Bearer <token>` header
- JWT secret mismatch → Ensure same secret between restarts

---

#### CORS Issues

**Symptom:**
```
Access to XMLHttpRequest blocked by CORS policy
```

**Solution:**
- Add your frontend origin to `SecurityConfig.java`
- Restart the application
- Clear browser cache

---

### Debugging Tips

**Enable SQL Logging:**
```properties
spring.jpa.show-sql=true
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

**Check Actuator Health:**
```bash
curl http://localhost:8081/actuator/health
```

**View All Endpoints:**
```bash
curl http://localhost:8081/actuator/mappings
```

---

## Performance & Scalability

### Database Optimization

**Recommended Indexes:**
```sql
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_access_keys_user_id ON access_keys(user_id);
CREATE INDEX idx_policies_user_id ON policies(user_id);
```

**Connection Pooling (HikariCP):**
```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
```

### Stateless Design

- No server-side sessions
- JWT stored client-side
- Horizontal scaling friendly
- Load balancer compatible

### Caching Strategy (Future)

- Cache user policies (Redis)
- Cache JWT public keys
- Cache access key lookups

---

## Security Best Practices

### Production Deployment Security

1. **Secrets Management**
   ```bash
   # Use AWS Secrets Manager, HashiCorp Vault, or K8s Secrets
   export JWT_SECRET=$(aws secretsmanager get-secret-value --secret-id iam-jwt-secret --query SecretString --output text)
   ```

2. **HTTPS Only**
   - Configure TLS termination at load balancer
   - Set `Secure` and `HttpOnly` flags on cookies (if used)

3. **Database Security**
   - Use strong passwords
   - Enable SSL/TLS for connections
   - Restrict network access (VPC, security groups)

4. **CORS Configuration**
   - Whitelist only trusted origins
   - Never use `*` in production

5. **Rate Limiting** (Future)
   ```java
   @RateLimiter(name = "loginLimiter", fallbackMethod = "rateLimitFallback")
   public AuthResponseDTO login(LoginRequestDTO request) { ... }
   ```

6. **Audit Logging**
   - Log all authentication attempts
   - Log access key creation/deletion
   - Retain logs for compliance

7. **Key Rotation**
   - Rotate JWT secret every 90 days
   - Rotate access keys regularly
   - Revoke old keys immediately

---

## Monitoring & Observability

### Actuator Endpoints

**Health:**
```bash
curl http://localhost:8081/actuator/health
```

**Metrics:**
```bash
curl http://localhost:8081/actuator/metrics
curl http://localhost:8081/actuator/metrics/jvm.memory.used
```

**Info:**
```bash
curl http://localhost:8081/actuator/info
```

### Logging Strategy

**Structured Logging (Future):**
```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

**Log Aggregation:**
- ELK Stack (Elasticsearch, Logstash, Kibana)
- Splunk
- Datadog

### Metrics (Future Enhancement)

**Prometheus Integration:**
```properties
management.metrics.export.prometheus.enabled=true
management.endpoints.web.exposure.include=health,info,prometheus
```

**Grafana Dashboard:**
- JVM metrics
- HTTP request rates
- Database connection pool
- NATS message throughput

---

## Contributing

### Development Setup

1. Fork the repository
2. Clone your fork
3. Create feature branch: `git checkout -b feature/my-feature`
4. Make changes
5. Run tests: `./mvnw test`
6. Commit: `git commit -m "feat: add my feature"`
7. Push: `git push origin feature/my-feature`
8. Create Pull Request

### Code Style

- Follow Java naming conventions
- Use Lombok annotations
- Add Javadoc for public methods
- Keep methods under 50 lines
- Use meaningful variable names

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add email verification
fix: resolve NATS connection timeout
docs: update API documentation
refactor: simplify JWT validation logic
test: add unit tests for PolicyService
```

### Pull Request Process

1. Ensure tests pass
2. Update documentation
3. Add test coverage for new features
4. Request review from maintainers
5. Address feedback
6. Merge after approval

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Roadmap

### Future Enhancements

**v2.0 (Planned)**
- [ ] Email verification for user registration
- [ ] Password reset via email
- [ ] Multi-factor authentication (TOTP)
- [ ] Refresh token support
- [ ] Rate limiting with Bucket4j
- [ ] Swagger/OpenAPI documentation
- [ ] API versioning (v1, v2)

**v3.0 (Future)**
- [ ] GraphQL API
- [ ] WebSocket support for real-time notifications
- [ ] OAuth2 integration (Google, GitHub)
- [ ] Role-based access control (RBAC)
- [ ] Audit log retention policies
- [ ] Key rotation automation
- [ ] Prometheus + Grafana integration

---

## Support & Contact

### Issue Reporting

Found a bug? Have a feature request?

1. Check [existing issues](https://github.com/your-org/iam-service/issues)
2. Create new issue with:
   - Clear description
   - Steps to reproduce (for bugs)
   - Expected vs actual behavior
   - Environment details

### Documentation

- **API Reference**: See [API Documentation](#api-documentation)
- **Architecture Guide**: See [Architecture](#architecture)
- **Deployment Guide**: See [Deployment](#deployment)

### Community

- **Discussions**: [GitHub Discussions](https://github.com/your-org/iam-service/discussions)
- **Discord**: [Join Server](https://discord.gg/your-server)
- **Email**: support@example.com

---

## Acknowledgments

Built with:
- [Spring Boot](https://spring.io/projects/spring-boot)
- [PostgreSQL](https://www.postgresql.org/)
- [NATS](https://nats.io/)
- [JJWT](https://github.com/jwtk/jjwt)

---

**Made with ❤️ by the SACCO Team**
