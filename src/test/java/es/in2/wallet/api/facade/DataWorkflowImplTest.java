package es.in2.wallet.api.facade;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.application.dto.CredentialsBasicInfo;
import es.in2.wallet.application.ports.VaultService;
import es.in2.wallet.application.workflows.data.impl.DataWorkflowImpl;
import es.in2.wallet.domain.enums.CredentialStatus;
import es.in2.wallet.domain.services.CredentialService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.ZonedDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataWorkflowImplTest {

    @Mock
    private CredentialService credentialService;

    @Mock
    private VaultService vaultService;

    @InjectMocks
    private DataWorkflowImpl userDataFacadeService;

    @Test
    void getUserVCs_UserExists_ReturnsVCs() throws JsonProcessingException {
        String processId = "process1";
        String userId = "user1";

        String jsonSubject = """
                    {
                        "credentialSubject": {
                            "id": "did:example:123"
                        }
                    }
                """;
        ObjectMapper objectMapper2 = new ObjectMapper();
        JsonNode credentialSubject = objectMapper2.readTree(jsonSubject);

        List<CredentialsBasicInfo> expectedCredentials = List.of(new CredentialsBasicInfo("id1", List.of("type"), CredentialStatus.VALID, List.of("jwt_vc", "cwt_vc"), credentialSubject, ZonedDateTime.now()));

        when(credentialService.getCredentialsByUserId(processId, userId)).thenReturn(Mono.just(expectedCredentials));

        StepVerifier.create(userDataFacadeService.getAllCredentialsByUserId(processId, userId))
                .expectNext(expectedCredentials)
                .verifyComplete();
        verify(credentialService).getCredentialsByUserId(processId, userId);
    }


    @Test
    void deleteVerifiableCredentialById_CredentialExists_DeletesCredential() {
        String processId = "process1";
        String userId = "user1";
        String credentialId = "cred1";
        String did = "did:example:123";

        when(credentialService.extractDidFromCredential(processId, credentialId, userId)).thenReturn(Mono.just(did));
        when(vaultService.deleteSecretByKey(did)).thenReturn(Mono.empty());
        when(credentialService.deleteCredential(processId, credentialId, userId)).thenReturn(Mono.empty());

        StepVerifier.create(userDataFacadeService.deleteCredentialByIdAndUserId(processId, credentialId, userId))
                .verifyComplete();

        verify(credentialService).extractDidFromCredential(processId, credentialId, userId);
        verify(vaultService).deleteSecretByKey(did);
        verify(credentialService).deleteCredential(processId, credentialId, userId);
    }

}

