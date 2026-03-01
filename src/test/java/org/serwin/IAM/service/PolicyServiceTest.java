package org.serwin.IAM.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serwin.iam.domain.Policy;
import org.serwin.iam.dto.DTOs.*;
import org.serwin.iam.repository.PolicyRepository;
import org.serwin.iam.repository.ProcessedRequestRepository;
import org.serwin.iam.service.PolicyService;

import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PolicyService Unit Tests")
class PolicyServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private ProcessedRequestRepository processedRequestRepository;

    @InjectMocks
    private PolicyService policyService;

    private Policy testPolicy;
    private UUID testPolicyId;
    private String testPrincipalId;

    @BeforeEach
    void setUp() {
        testPolicyId = UUID.randomUUID();
        testPrincipalId = "lambda-123";

        testPolicy = new Policy();
        testPolicy.setId(testPolicyId);
        testPolicy.setAccountId("user-1");
        testPolicy.setPrincipalId(testPrincipalId);
        testPolicy.setResourceType("s3");
        testPolicy.setResourceId("bucket-1");
        testPolicy.setAction("PutObject");
        testPolicy.setCreatedBy("user-1");
        testPolicy.setCreatedAt(OffsetDateTime.now());
    }

    @Test
    @DisplayName("Should successfully create a new policy")
    void createPolicy_Success() {
        // Arrange
        PolicyCreateEvent event = new PolicyCreateEvent("req-1", "user-1", "lambda-1", "s3", "bucket-1", "PutObject",
                "user-1");
        when(processedRequestRepository.existsById("req-1")).thenReturn(false);
        when(policyRepository.existsByPrincipalIdAndResourceTypeAndResourceIdAndAction("lambda-1", "s3", "bucket-1",
                "PutObject")).thenReturn(false);
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> {
            Policy savedPolicy = invocation.getArgument(0);
            savedPolicy.setId(testPolicyId);
            return savedPolicy;
        });

        // Act
        PolicyResponseDTO result = policyService.createPolicy(event);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(testPolicyId);
        verify(policyRepository).save(any(Policy.class));
        verify(processedRequestRepository).save(any());
    }

    @Test
    @DisplayName("Should successfully update a policy")
    void updatePolicy_Success() {
        // Arrange
        PolicyUpdateEvent event = new PolicyUpdateEvent("req-2", testPolicyId.toString(), "s3", "bucket-2",
                "GetObject");
        when(processedRequestRepository.existsById("req-2")).thenReturn(false);
        when(policyRepository.findById(testPolicyId)).thenReturn(Optional.of(testPolicy));
        when(policyRepository.save(any(Policy.class))).thenReturn(testPolicy);

        // Act
        PolicyResponseDTO result = policyService.updatePolicy(event);

        // Assert
        assertThat(result).isNotNull();
        verify(policyRepository).save(any(Policy.class));
        verify(processedRequestRepository).save(any());
    }

    @Test
    @DisplayName("Should ignore create on idempotent request")
    void createPolicy_Idempotent_Throws() {
        // Arrange
        PolicyCreateEvent event = new PolicyCreateEvent("req-1", "user-1", "lambda-1", "s3", "bucket-1", "PutObject",
                "user-1");
        when(processedRequestRepository.existsById("req-1")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> policyService.createPolicy(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("idempotent_request_processed");

        verify(policyRepository, never()).save(any(Policy.class));
    }
}
