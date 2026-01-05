# Master Prompt for Generating IAM Service

**Instruction**: Copy and paste the entire block below into your AI coding assistant (Claude Code or similar).

---

**Role**: You are a Principal Java Engineer standardizing a microservices architecture.

**Objective**:
Build a production-ready **Centralized IAM Service** using **Spring Boot 4** and **Java 17**.
This service acts as both the **Identity Provider (IdP)** (Signup/Login) and the **Authorization Authority** (Access Keys & Policies) for an AWS clone ecosystem (S3, EC2, etc.).

**Technical Constraints (Must Follow)**:

1.  **Architecture**: Strict **Controller-Service-Repository** layer separation.
2.  **Data Handling**:
    - Use **JPA Entities** for Database persistence.
    - Use **DTOs** (Records or Classes) for all API Request/Response bodies.
    - **NEVER** return an Entity directly from a Controller. Manual mapping or ModelMapper is fine.
3.  **Communication**:
    - **HTTP (REST)**: For human/frontend interactions (Signup, Key Management).
    - **NATS (Messaging)**: For machine/service interactions (Validation of keys by S3 service).
4.  **Database**: PostgreSQL.
5.  **Security**:
    - Use `JJWT` or similar for JWT generation on login.
    - Password hashing (BCrypt).

---

## 1. Domain Models (Entities)

- **User**: `id` (UUID), `email` (Unique), `passwordHash`, `createdAt`.
- **AccessKey**: `accessKeyId` (20-char numeric string, e.g., "AKIA..."), `secretAccessKey` (40-char string), `status` (ACTIVE/INACTIVE), `user` (Many-to-One).
- **Policy**: `id`, `name`, `policyDocument` (TEXT/JSON - stores the AWS-style JSON permissions), `user` (Many-to-One).

## 2. API Schema (REST Controllers)

### A. AuthController (Identity)

- `POST /auth/register`
  - **Input**: `RegisterRequestDTO` (email, password).
  - **Logic**: Hash password, save User.
  - **Output**: `AuthResponseDTO` (token, generic success message).
- `POST /auth/login`
  - **Input**: `LoginRequestDTO` (email, password).
  - **Logic**: Verify hash, generate **JWT** (Subject: userId).
  - **Output**: `AuthResponseDTO` (token).

### B. AccessKeyController (Management)

- `GET /api/keys`
  - **Headers**: `Authorization: Bearer <JWT>`
  - **Output**: `List<AccessKeyResponseDTO>` (Hide the secret key!).
- `POST /api/keys`
  - **Headers**: `Authorization: Bearer <JWT>`
  - **Output**: `AccessKeyCreatedDTO` (Show AccessKey AND SecretKey - one time only).

### C. PolicyController (Authorization)

- `POST /api/policies`
  - **Input**: `CreatePolicyDTO` (name, JSON document).
  - **Logic**: Validate JSON structure, save.

---

## 3. NATS Integration (The Authorization Engine)

This is how other services (like S3) check if a request is valid.

- **Topic**: `iam.auth.validate`
- **Pattern**: Request-Reply.
- **Message Listener**:
  - **Input**: JSON `{ "accessKeyId": "AKIA..." }`
  - **Logic**:
    1.  Find `AccessKey` by public URI.
    2.  If invalid/inactive -> Return `{ "valid": false }`.
    3.  If valid -> Fetch `User` and all attached `Policy` entities.
  - **Output**:
    ```json
    {
      "valid": true,
      "userId": "uuid-of-user",
      "secretKey": "raw-secret-key-for-signature-check",
      "policies": ["json-string-1", "json-string-2"]
    }
    ```

---

## 4. Implementation Checklist

Please generate the full project structure including:

1.  `pom.xml` (Deps: Spring Web, Data JPA, Postgres, Lombok, JJWT, JNats).
2.  `docker-compose.yml` (Postgres + NATS).
3.  **Entity Classes** (User, AccessKey, Policy).
4.  **Repository Interfaces**.
5.  **DTOs**.
6.  **Service Classes** (Business logic for hashing, key generation).
7.  **Controllers** (REST endpoints).
8.  **NatsListener** (The validation logic).

**Final Note to AI**: Focus on writing clean, production-grade Java code. Ensure `Application.properties` is configured for local Docker checks.
