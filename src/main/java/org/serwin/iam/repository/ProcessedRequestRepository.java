package org.serwin.iam.repository;

import org.serwin.iam.domain.ProcessedRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedRequestRepository extends JpaRepository<ProcessedRequest, String> {
}
