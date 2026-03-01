package org.serwin.iam.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "processed_requests")
@Data
@NoArgsConstructor
public class ProcessedRequest {

    @Id
    private String requestId;

    @Column(nullable = false)
    private OffsetDateTime processedAt;
}
