package org.serwin.iam.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

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
