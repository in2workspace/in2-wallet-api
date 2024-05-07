package es.in2.wallet.api.facade;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.application.port.BrokerService;
import es.in2.wallet.application.port.VaultService;
import es.in2.wallet.application.service.impl.DataWorkflowService;
import es.in2.wallet.domain.model.CredentialStatus;
import es.in2.wallet.domain.model.CredentialsBasicInfo;
import es.in2.wallet.domain.service.DataService;
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
class DataWorkflowServiceTest {

    @Mock
    private BrokerService brokerService;

    @Mock
    private DataService dataService;

    @Mock
    private VaultService vaultService;

    @InjectMocks
    private DataWorkflowService userDataFacadeService;

    @Test
    void getUserVCs_UserExists_ReturnsVCs() throws JsonProcessingException {
        String processId = "process1";
        String userId = "user1";
        String credentials = "user credentials";


        String jsonSubject = """
            {
                "credentialSubject": {
                    "id": "did:example:123"
                }
            }
        """;
        ObjectMapper objectMapper2 = new ObjectMapper();
        JsonNode credentialSubject = objectMapper2.readTree(jsonSubject);

        List<CredentialsBasicInfo> expectedCredentials = List.of(new CredentialsBasicInfo("id1", List.of("type"), CredentialStatus.VALID,List.of("jwt_vc","cwt_vc"),credentialSubject, ZonedDateTime.now()));

        when(brokerService.getAllCredentialsByUserId(processId, userId)).thenReturn(Mono.just(credentials));
        when(dataService.getUserVCsInJson(credentials)).thenReturn(Mono.just(expectedCredentials));

        StepVerifier.create(userDataFacadeService.getAllCredentialsByUserId(processId, userId))
                .expectNext(expectedCredentials)
                .verifyComplete();

        verify(brokerService).getAllCredentialsByUserId(processId, userId);
        verify(dataService).getUserVCsInJson(credentials);
    }


    @Test
    void deleteVerifiableCredentialById_CredentialExists_DeletesCredential() {
        String processId = "process1";
        String userId = "user1";
        String credentialId = "cred1";
        String did = "did:example:123";
        String credentialEntity = "credential";

        when(brokerService.getCredentialByAndUserId(processId, userId,credentialId)).thenReturn(Mono.just(credentialEntity));
        when(dataService.extractDidFromVerifiableCredential(credentialEntity)).thenReturn(Mono.just(did));
        when(vaultService.deleteSecretByKey(did)).thenReturn(Mono.empty());
        when(brokerService.deleteCredentialByIdAndUserId(processId, userId, credentialId)).thenReturn(Mono.empty());

        StepVerifier.create(userDataFacadeService.deleteCredentialById(processId, credentialId, userId))
                .verifyComplete();

        verify(brokerService).getCredentialByAndUserId(processId, userId,credentialId);
        verify(dataService).extractDidFromVerifiableCredential(credentialEntity);
        verify(vaultService).deleteSecretByKey(did);
        verify(brokerService).deleteCredentialByIdAndUserId(processId, userId, credentialId);
    }

}

