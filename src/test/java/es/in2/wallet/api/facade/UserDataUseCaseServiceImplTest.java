package es.in2.wallet.api.facade;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.application.service.impl.UserDataUseCaseServiceImpl;
import es.in2.wallet.domain.model.CredentialsBasicInfoWithExpirationDate;
import es.in2.wallet.domain.service.UserDataService;
import es.in2.wallet.application.port.VaultService;
import es.in2.wallet.application.port.BrokerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDataUseCaseServiceImplTest {

    @Mock
    private BrokerService brokerService;

    @Mock
    private UserDataService userDataService;

    @Mock
    private VaultService vaultService;

    @InjectMocks
    private UserDataUseCaseServiceImpl userDataFacadeService;

    @Test
    void getUserVCs_UserExists_ReturnsVCs() throws JsonProcessingException {
        String processId = "process1";
        String userId = "user1";
        String userEntityString = "entity";
        Optional<String> userEntity = Optional.of(userEntityString);


        String json = "{\"id\":\"subjectId\"}";
        ObjectMapper objectMapper2 = new ObjectMapper();
        JsonNode credentialSubject = objectMapper2.readTree(json);

        List<CredentialsBasicInfoWithExpirationDate> expectedCredentials = List.of(new CredentialsBasicInfoWithExpirationDate("id1", List.of("type"), credentialSubject, ZonedDateTime.now()));

        when(brokerService.getEntityById(processId, userId)).thenReturn(Mono.just(userEntity));
        when(userDataService.getUserVCsInJson(anyString())).thenReturn(Mono.just(expectedCredentials));

        StepVerifier.create(userDataFacadeService.getUserVCs(processId, userId))
                .expectNext(expectedCredentials)
                .verifyComplete();

        verify(brokerService).getEntityById(processId, userId);
        verify(userDataService).getUserVCsInJson(userEntity.get());
    }

    @Test
    void getUserVCs_UserDoesNotExist_ReturnsEmptyList() {
        String processId = "process1";
        String userId = "user1";
        List<CredentialsBasicInfoWithExpirationDate> expectedCredentials = List.of();
        when(brokerService.getEntityById(processId, userId)).thenReturn(Mono.just(Optional.empty()));

        StepVerifier.create(userDataFacadeService.getUserVCs(processId, userId))
                .expectNext(expectedCredentials)
                .verifyComplete();
        verify(brokerService).getEntityById(processId, userId);
        verifyNoInteractions(userDataService);
    }

    @Test
    void deleteVerifiableCredentialById_CredentialExists_DeletesCredential() {
        String processId = "process1";
        String userId = "user1";
        String credentialId = "cred1";
        String did = "did:example:123";
        String userEntityString = "entity";
        Optional<String> userEntity = Optional.of(userEntityString);
        String updatedEntity = "updatedEntity";

        when(brokerService.getEntityById(processId, userId)).thenReturn(Mono.just(userEntity));
        when(userDataService.extractDidFromVerifiableCredential(anyString(), eq(credentialId))).thenReturn(Mono.just(did));
        when(vaultService.deleteSecretByKey(did)).thenReturn(Mono.empty());
        when(userDataService.deleteVerifiableCredential(anyString(), eq(credentialId), eq(did))).thenReturn(Mono.just(updatedEntity));
        when(brokerService.updateEntity(processId, userId, updatedEntity)).thenReturn(Mono.empty());

        StepVerifier.create(userDataFacadeService.deleteVerifiableCredentialById(processId, credentialId, userId))
                .verifyComplete();

        verify(brokerService).getEntityById(processId, userId);
        verify(userDataService).extractDidFromVerifiableCredential(userEntity.get(), credentialId);
        verify(vaultService).deleteSecretByKey(did);
        verify(userDataService).deleteVerifiableCredential(userEntity.get(), credentialId, did);
        verify(brokerService).updateEntity(processId, userId, updatedEntity);
    }

}

