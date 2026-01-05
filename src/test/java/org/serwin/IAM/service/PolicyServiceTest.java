package org.serwin.IAM.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serwin.iam.domain.Policy;
import org.serwin.iam.domain.User;
import org.serwin.iam.dto.DTOs.*;
import org.serwin.iam.repository.PolicyRepository;
import org.serwin.iam.service.PolicyService;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PolicyService Unit Tests")
class PolicyServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    @InjectMocks
    private PolicyService policyService;

    private User testUser;
    private User otherUser;
    private Policy testPolicy;
    private UUID testUserId;
    private UUID otherUserId;
    private UUID testPolicyId;
    private String testPolicyName;
    private String testPolicyDocument;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        testPolicyId = UUID.randomUUID();
        testPolicyName = "S3ReadOnlyAccess";
        testPolicyDocument = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Action": ["s3:GetObject", "s3:ListBucket"],
                      "Resource": "*"
                    }
                  ]
                }
                """;

        testUser = new User();
        testUser.setId(testUserId);
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hash");
        testUser.setCreatedAt(LocalDateTime.now());

        otherUser = new User();
        otherUser.setId(otherUserId);
        otherUser.setEmail("other@example.com");
        otherUser.setPasswordHash("hash");
        otherUser.setCreatedAt(LocalDateTime.now());

        testPolicy = new Policy();
        testPolicy.setId(testPolicyId);
        testPolicy.setName(testPolicyName);
        testPolicy.setPolicyDocument(testPolicyDocument);
        testPolicy.setUser(testUser);
    }

    @Test
    @DisplayName("Should successfully create a new policy")
    void createPolicy_Success() {
        // Arrange
        CreatePolicyDTO dto = new CreatePolicyDTO(testPolicyName, testPolicyDocument);
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> {
            Policy savedPolicy = invocation.getArgument(0);
            savedPolicy.setId(testPolicyId);
            return savedPolicy;
        });

        // Act
        PolicyResponseDTO result = policyService.createPolicy(testUser, dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(testPolicyId);
        assertThat(result.name()).isEqualTo(testPolicyName);
        assertThat(result.policyDocument()).isEqualTo(testPolicyDocument);

        verify(policyRepository).save(argThat(policy ->
                policy.getName().equals(testPolicyName) &&
                policy.getPolicyDocument().equals(testPolicyDocument) &&
                policy.getUser().equals(testUser)
        ));
    }

    @Test
    @DisplayName("Should create policy with complex JSON document")
    void createPolicy_ComplexDocument() {
        // Arrange
        String complexDocument = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Sid": "AllowS3ReadWrite",
                      "Effect": "Allow",
                      "Action": ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"],
                      "Resource": "arn:aws:s3:::my-bucket/*",
                      "Condition": {
                        "StringEquals": {
                          "s3:x-amz-server-side-encryption": "AES256"
                        }
                      }
                    }
                  ]
                }
                """;
        CreatePolicyDTO dto = new CreatePolicyDTO("ComplexPolicy", complexDocument);
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> {
            Policy savedPolicy = invocation.getArgument(0);
            savedPolicy.setId(testPolicyId);
            return savedPolicy;
        });

        // Act
        PolicyResponseDTO result = policyService.createPolicy(testUser, dto);

        // Assert
        assertThat(result.policyDocument()).isEqualTo(complexDocument);
    }

    @Test
    @DisplayName("Should successfully list all policies for a user")
    void listPolicies_Success() {
        // Arrange
        Policy policy2 = new Policy();
        policy2.setId(UUID.randomUUID());
        policy2.setName("S3FullAccess");
        policy2.setPolicyDocument("{\"Version\": \"2012-10-17\"}");
        policy2.setUser(testUser);

        List<Policy> policies = Arrays.asList(testPolicy, policy2);
        when(policyRepository.findByUserId(testUserId)).thenReturn(policies);

        // Act
        List<PolicyResponseDTO> result = policyService.listPolicies(testUser);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(testPolicyId);
        assertThat(result.get(0).name()).isEqualTo(testPolicyName);
        assertThat(result.get(1).name()).isEqualTo("S3FullAccess");

        verify(policyRepository).findByUserId(testUserId);
    }

    @Test
    @DisplayName("Should return empty list when user has no policies")
    void listPolicies_NoPolicies() {
        // Arrange
        when(policyRepository.findByUserId(testUserId)).thenReturn(Collections.emptyList());

        // Act
        List<PolicyResponseDTO> result = policyService.listPolicies(testUser);

        // Assert
        assertThat(result).isEmpty();
        verify(policyRepository).findByUserId(testUserId);
    }

    @Test
    @DisplayName("Should successfully get a specific policy by ID")
    void getPolicy_Success() {
        // Arrange
        when(policyRepository.findById(testPolicyId)).thenReturn(Optional.of(testPolicy));

        // Act
        PolicyResponseDTO result = policyService.getPolicy(testUser, testPolicyId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(testPolicyId);
        assertThat(result.name()).isEqualTo(testPolicyName);
        assertThat(result.policyDocument()).isEqualTo(testPolicyDocument);

        verify(policyRepository).findById(testPolicyId);
    }

    @Test
    @DisplayName("Should throw exception when getting non-existent policy")
    void getPolicy_PolicyNotFound() {
        // Arrange
        when(policyRepository.findById(testPolicyId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> policyService.getPolicy(testUser, testPolicyId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Policy not found");

        verify(policyRepository).findById(testPolicyId);
    }

    @Test
    @DisplayName("Should throw exception when getting another user's policy")
    void getPolicy_Unauthorized() {
        // Arrange
        testPolicy.setUser(otherUser);
        when(policyRepository.findById(testPolicyId)).thenReturn(Optional.of(testPolicy));

        // Act & Assert
        assertThatThrownBy(() -> policyService.getPolicy(testUser, testPolicyId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unauthorized: You don't own this policy");

        verify(policyRepository).findById(testPolicyId);
    }

    @Test
    @DisplayName("Should successfully delete own policy")
    void deletePolicy_Success() {
        // Arrange
        when(policyRepository.findById(testPolicyId)).thenReturn(Optional.of(testPolicy));

        // Act
        policyService.deletePolicy(testUser, testPolicyId);

        // Assert
        verify(policyRepository).findById(testPolicyId);
        verify(policyRepository).delete(testPolicy);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent policy")
    void deletePolicy_PolicyNotFound() {
        // Arrange
        when(policyRepository.findById(testPolicyId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> policyService.deletePolicy(testUser, testPolicyId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Policy not found");

        verify(policyRepository).findById(testPolicyId);
        verify(policyRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw exception when deleting another user's policy")
    void deletePolicy_Unauthorized() {
        // Arrange
        testPolicy.setUser(otherUser);
        when(policyRepository.findById(testPolicyId)).thenReturn(Optional.of(testPolicy));

        // Act & Assert
        assertThatThrownBy(() -> policyService.deletePolicy(testUser, testPolicyId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unauthorized: You don't own this policy");

        verify(policyRepository).findById(testPolicyId);
        verify(policyRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should verify policy is saved with correct user association")
    void createPolicy_UserAssociation() {
        // Arrange
        CreatePolicyDTO dto = new CreatePolicyDTO(testPolicyName, testPolicyDocument);
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> {
            Policy savedPolicy = invocation.getArgument(0);
            assertThat(savedPolicy.getUser()).isEqualTo(testUser);
            assertThat(savedPolicy.getUser().getId()).isEqualTo(testUserId);
            savedPolicy.setId(testPolicyId);
            return savedPolicy;
        });

        // Act
        policyService.createPolicy(testUser, dto);

        // Assert
        verify(policyRepository).save(any(Policy.class));
    }

    @Test
    @DisplayName("Should create multiple policies for the same user")
    void createPolicy_MultiplePolicies() {
        // Arrange
        CreatePolicyDTO dto1 = new CreatePolicyDTO("Policy1", "{\"Version\": \"2012-10-17\"}");
        CreatePolicyDTO dto2 = new CreatePolicyDTO("Policy2", "{\"Version\": \"2012-10-17\"}");
        CreatePolicyDTO dto3 = new CreatePolicyDTO("Policy3", "{\"Version\": \"2012-10-17\"}");

        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> {
            Policy savedPolicy = invocation.getArgument(0);
            savedPolicy.setId(UUID.randomUUID());
            return savedPolicy;
        });

        // Act
        PolicyResponseDTO result1 = policyService.createPolicy(testUser, dto1);
        PolicyResponseDTO result2 = policyService.createPolicy(testUser, dto2);
        PolicyResponseDTO result3 = policyService.createPolicy(testUser, dto3);

        // Assert
        assertThat(result1.name()).isEqualTo("Policy1");
        assertThat(result2.name()).isEqualTo("Policy2");
        assertThat(result3.name()).isEqualTo("Policy3");
        verify(policyRepository, times(3)).save(any(Policy.class));
    }

    @Test
    @DisplayName("Should handle policy with minimal JSON document")
    void createPolicy_MinimalDocument() {
        // Arrange
        String minimalDocument = "{}";
        CreatePolicyDTO dto = new CreatePolicyDTO("MinimalPolicy", minimalDocument);
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> {
            Policy savedPolicy = invocation.getArgument(0);
            savedPolicy.setId(testPolicyId);
            return savedPolicy;
        });

        // Act
        PolicyResponseDTO result = policyService.createPolicy(testUser, dto);

        // Assert
        assertThat(result.policyDocument()).isEqualTo(minimalDocument);
    }

    @Test
    @DisplayName("Should list policies only for specific user")
    void listPolicies_UserIsolation() {
        // Arrange
        Policy userPolicy = new Policy();
        userPolicy.setId(UUID.randomUUID());
        userPolicy.setName("UserPolicy");
        userPolicy.setPolicyDocument("{}");
        userPolicy.setUser(testUser);

        when(policyRepository.findByUserId(testUserId)).thenReturn(Collections.singletonList(userPolicy));

        // Act
        List<PolicyResponseDTO> result = policyService.listPolicies(testUser);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("UserPolicy");
        verify(policyRepository).findByUserId(testUserId);
        verify(policyRepository, never()).findByUserId(otherUserId);
    }

    @Test
    @DisplayName("Should preserve policy document formatting")
    void createPolicy_PreservesFormatting() {
        // Arrange
        String formattedDocument = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Action": "s3:*",
                      "Resource": "*"
                    }
                  ]
                }
                """;
        CreatePolicyDTO dto = new CreatePolicyDTO("FormattedPolicy", formattedDocument);
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> {
            Policy savedPolicy = invocation.getArgument(0);
            savedPolicy.setId(testPolicyId);
            return savedPolicy;
        });

        // Act
        PolicyResponseDTO result = policyService.createPolicy(testUser, dto);

        // Assert
        assertThat(result.policyDocument()).isEqualTo(formattedDocument);
    }

    @Test
    @DisplayName("Should verify authorization check before get operation")
    void getPolicy_AuthorizationCheck() {
        // Arrange
        testPolicy.setUser(otherUser);
        when(policyRepository.findById(testPolicyId)).thenReturn(Optional.of(testPolicy));

        // Act & Assert
        assertThatThrownBy(() -> policyService.getPolicy(testUser, testPolicyId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unauthorized");

        verify(policyRepository).findById(testPolicyId);
    }

    @Test
    @DisplayName("Should verify authorization check before delete operation")
    void deletePolicy_AuthorizationCheck() {
        // Arrange
        testPolicy.setUser(otherUser);
        when(policyRepository.findById(testPolicyId)).thenReturn(Optional.of(testPolicy));

        // Act & Assert
        assertThatThrownBy(() -> policyService.deletePolicy(testUser, testPolicyId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unauthorized");

        verify(policyRepository).findById(testPolicyId);
        verify(policyRepository, never()).delete(any());
    }
}
