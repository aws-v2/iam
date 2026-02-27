package org.serwin.iam.repository;

import org.serwin.iam.domain.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, UUID> {
    List<Policy> findByUserId(UUID userId);

    Optional<Policy> findByAccountIdAndName(String accountId, String name);

    boolean existsByAccountIdAndName(String accountId, String name);

    void deleteByAccountIdAndName(String accountId, String name);
}
