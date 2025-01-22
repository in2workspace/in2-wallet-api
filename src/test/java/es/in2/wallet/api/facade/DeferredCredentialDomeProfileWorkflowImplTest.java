package es.in2.wallet.api.facade;

import es.in2.wallet.application.dto.CredentialResponse;
import es.in2.wallet.application.dto.CredentialResponseWithStatus;
import es.in2.wallet.application.workflows.issuance.impl.DeferredCredentialDomeProfileWorkflowImpl;
import es.in2.wallet.domain.entities.DeferredCredentialMetadata;
import es.in2.wallet.domain.services.CredentialService;
import es.in2.wallet.infrastructure.services.CredentialRepositoryService;
import es.in2.wallet.infrastructure.services.DeferredCredentialMetadataRepositoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static es.in2.wallet.domain.utils.ApplicationConstants.JWT_VC;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeferredCredentialDomeProfileWorkflowImplTest {

    @Mock
    private CredentialService credentialService;
    @Mock
    private CredentialRepositoryService credentialRepositoryService;
    @Mock
    private DeferredCredentialMetadataRepositoryService deferredCredentialMetadataRepositoryService;

    @InjectMocks
    private DeferredCredentialDomeProfileWorkflowImpl service;

    @Test
    void requestDeferredCredential_withNewCredential_Success(){
        String processId = "processId";
        String userId = UUID.randomUUID().toString();
        String credentialId = UUID.randomUUID().toString();
        UUID credentialUuid = UUID.fromString(credentialId);
        UUID transactionUuid = UUID.randomUUID();
        String accessToken = "access123";
        String deferredEndpoint = "https://example.com/callback";

        DeferredCredentialMetadata deferredCredentialMetadata = DeferredCredentialMetadata.builder()
                .credentialId(credentialUuid)
                .transactionId(transactionUuid)
                .accessToken(accessToken)
                .deferredEndpoint(deferredEndpoint)
                .build();

        CredentialResponse credentialResponse = CredentialResponse.builder()
                .credential("ey134...")
                .format(JWT_VC)
                .build();

        CredentialResponseWithStatus credentialResponseWithStatus = CredentialResponseWithStatus.builder()
                .credentialResponse(credentialResponse)
                .build();

        when(deferredCredentialMetadataRepositoryService.getDeferredCredentialMetadataByCredentialId(processId, credentialId))
                .thenReturn(Mono.just(deferredCredentialMetadata));

        when(credentialService.getCredentialDomeDeferredCase(transactionUuid.toString(),accessToken,deferredEndpoint)).thenReturn(Mono.just(credentialResponseWithStatus));

        when(credentialRepositoryService.saveDeferredCredential(processId, userId, credentialId, credentialResponse))
                .thenReturn(Mono.empty());

        when(deferredCredentialMetadataRepositoryService.deleteDeferredCredentialMetadataByCredentialId(processId, credentialId))
                .thenReturn(Mono.empty());

        StepVerifier.create(service.requestDeferredCredential(processId, userId, credentialId))
                .verifyComplete();
    }

    @Test
    void requestDeferredCredential_withNewTransactionId_Success(){
        String processId = "processId";
        String userId = UUID.randomUUID().toString();
        String credentialId = UUID.randomUUID().toString();
        UUID credentialUuid = UUID.fromString(credentialId);
        UUID transactionUuid = UUID.randomUUID();
        String newTransactionUuid = UUID.randomUUID().toString();
        String accessToken = "access123";
        String deferredEndpoint = "https://example.com/callback";

        DeferredCredentialMetadata deferredCredentialMetadata = DeferredCredentialMetadata.builder()
                .credentialId(credentialUuid)
                .transactionId(transactionUuid)
                .accessToken(accessToken)
                .deferredEndpoint(deferredEndpoint)
                .build();

        CredentialResponse credentialResponse = CredentialResponse.builder()
                .transactionId(newTransactionUuid)
                .build();

        CredentialResponseWithStatus credentialResponseWithStatus = CredentialResponseWithStatus.builder()
                .credentialResponse(credentialResponse)
                .build();

        when(deferredCredentialMetadataRepositoryService.getDeferredCredentialMetadataByCredentialId(processId, credentialId))
                .thenReturn(Mono.just(deferredCredentialMetadata));

        when(credentialService.getCredentialDomeDeferredCase(transactionUuid.toString(),accessToken,deferredEndpoint)).thenReturn(Mono.just(credentialResponseWithStatus));

        when(deferredCredentialMetadataRepositoryService.updateDeferredCredentialMetadataTransactionIdByCredentialId(processId, credentialId, newTransactionUuid))
                .thenReturn(Mono.empty());

        StepVerifier.create(service.requestDeferredCredential(processId, userId, credentialId))
                .verifyComplete();
    }
}

