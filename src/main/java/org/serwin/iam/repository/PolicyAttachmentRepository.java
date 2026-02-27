package org.serwin.iam.repository;

import org.serwin.iam.domain.PolicyAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PolicyAttachmentRepository extends JpaRepository<PolicyAttachment, UUID> {
}
