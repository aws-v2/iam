package org.serwin.iam.repository;

import org.serwin.iam.domain.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, UUID> {
    List<Policy> findByPrincipalId(String principalId);

    Optional<Policy> findByPrincipalIdAndResourceTypeAndResourceIdAndAction(
            String principalId, String resourceType, String resourceId, String action);

    boolean existsByPrincipalIdAndResourceTypeAndResourceIdAndAction(
            String principalId, String resourceType, String resourceId, String action);

    void deleteByPrincipalIdAndResourceTypeAndResourceIdAndAction(
            String principalId, String resourceType, String resourceId, String action);
}
