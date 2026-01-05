package org.serwin.iam.repository;

import org.serwin.iam.domain.AccessKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccessKeyRepository extends JpaRepository<AccessKey, String> {
    List<AccessKey> findByUserId(java.util.UUID userId);

    // For validation
    Optional<AccessKey> findByAccessKeyId(String accessKeyId);
}
