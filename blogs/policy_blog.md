I want you to rewrite the entire IAM policy system for my mini-cloud project.


1. Goals

Support multi-tenancy: multiple users can create multiple principals (Lambdas, EC2, apps).

Each principal has its own unique policies stored in the database.

Policies are per-principal, per-resource, per-action.

All microservices (Lambda, EC2, S3, RDS, etc.) must query the IAM service to check permissions using NATS messaging.

System should allow CRUD operations for policies.

2. Domain Model

Policy Entity Example:

@Entity
@Table(name = "policies", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "principalId", "resourceId", "resourceType", "action" })
})
@Data
@NoArgsConstructor
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String accountId; // user account id

    @Column(nullable = false)
    private String principalId; // lambda, EC2, app ID

    @Column(nullable = false)
    private String resourceType; // e.g., "s3", "rds", "ec2"

    @Column(nullable = false)
    private String resourceId; // e.g., bucket ARN or database id

    @Column(nullable = false)
    private String action; // e.g., PutObject, GetObject, UpdateDatabase

    @Column(nullable = false)
    private String createdBy; // user id

    @Column(nullable = false)
    private java.time.OffsetDateTime createdAt;

    private java.time.OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // optional, null for system/service-created policies
}

Notes:

Each row corresponds to one action on one resource for one principal.

Avoid storing multiple resource types or actions as arrays in the database. Keep it normalized.

3. Features to Implement

3.1. CRUD Operations for Policies

createPolicy(PolicyCreateEvent event) → registers a new policy for a principal.

updatePolicy(PolicyUpdateEvent event) → updates action/resource for an existing principal.

deletePolicy(principalId, resourceType, resourceId, action) → removes a policy row.

getPoliciesByPrincipal(principalId) → returns all policies for a principal.


3.3. NATS Event Integration

The system should listen to NATS subjects for policy CRUD requests.

Example NATS subject format:

<env>.<service>.<version>.<domain>.<action>
dev.iam.v1.policy.create
dev.iam.v1.policy.update
dev.iam.v1.policy.delete
dev.iam.v1.policy.get

Publish response messages back with success/failure and request ID.

4. DTOs / Event Classes

PolicyCreateEvent: contains principalId, resourceType, resourceId, action, createdBy, requestId

PolicyUpdateEvent: contains policyId, new resource/action if updating

PolicyResponseDTO: contains policy details to return to requesting services

Example PolicyCreateEvent JSON:

{
  "request_id": "req-001",
  "account_id": "user-123",
  "principal_id": "lambda-123",
  "resource_type": "s3",
  "resource_id": "bucket/alice-images/*",
  "action": "PutObject",
  "created_by": "user-123"
}
5. Service Layer Requirements

Implement PolicyService to handle all CRUD logic.

Must handle idempotency, validation, uniqueness.

Must integrate with PolicyRepository and ProcessedRequestRepository for idempotency.

Use transactional annotations where necessary.

6. Repository Layer

PolicyRepository: JPA repository for Policy

ProcessedRequestRepository: tracks request IDs to ensure idempotency

7. Validation Rules

principalId, resourceType, resourceId, and action cannot be null

resourceType must be one of allowed types: "s3", "rds", "ec2", "lambda"

action must match allowed actions for the resource type

Example: S3 → PutObject/GetObject/DeleteObject

RDS → UpdateDatabase/ReadDatabase

8. Logging

Log each NATS message received

Log policy creation, update, deletion, retrieval

Log errors with requestId and principalId

9. Example Use Case

User registers a Lambda lambda-123

User assigns a policy to allow S3 PutObject to bucket alice-images

Lambda tries to upload a file → sends NATS event to IAM

IAM validates policy → allows or denies request

10. Requirements for the Implementation

Use Spring Boot + JPA/Hibernate

Use Lombok for boilerplate

Include domain classes, DTOs, repositories, services

Include NATS message listener class for policy events

Include full CRUD methods for policies

Include transactional handling and idempotency
