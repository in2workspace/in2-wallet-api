package es.in2.wallet.infrastructure.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.Payload;
import com.nimbusds.jwt.SignedJWT;
import es.in2.wallet.application.dto.CredentialResponse;
import es.in2.wallet.application.dto.CredentialsBasicInfo;
import es.in2.wallet.domain.entities.Credential;
import es.in2.wallet.domain.enums.CredentialFormats;
import es.in2.wallet.domain.enums.CredentialStatus;
import es.in2.wallet.infrastructure.repositories.CredentialRepository;
import es.in2.wallet.infrastructure.services.impl.CredentialRepositoryServiceImp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CredentialRepositoryServiceImpTest {

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CredentialRepositoryServiceImp credentialRepositoryService;

    @Test
    void testSaveCredential_PlainJsonFormat_Success() throws JsonProcessingException {
        // GIVEN
        String processId = "proc123";
        UUID userId = UUID.randomUUID();
        String credential = "credential";
        String credentialId = "8c7a6213-544d-450d-8e3d-b41fa9009198";
        CredentialResponse response = CredentialResponse.builder()
                .format(null)
                .credential(credential)
                .build();

        // Suppose the repository will return a saved credential with a known ID
        Credential savedEntity = Credential.builder()
                .credentialId(UUID.fromString(credentialId))
                .userId(userId)
                .build();

        // We'll capture the credential passed into save(...)
        ArgumentCaptor<Credential> captor = ArgumentCaptor.forClass(Credential.class);
        when(credentialRepository.save(captor.capture()))
                .thenReturn(Mono.just(savedEntity));

        when(objectMapper.readTree(credential)).thenReturn(getJsonNodeCredentialLearCredentialEmployee());

        // WHEN
        Mono<UUID> result = credentialRepositoryService.saveCredential(processId, userId, response);

        // THEN
        StepVerifier.create(result)
                .expectNext(UUID.fromString(credentialId))
                .verifyComplete();

        // Verify the repository was called exactly once
        verify(credentialRepository).save(any(Credential.class));

        // Check the captured entity's fields
        Credential passedToSave = captor.getValue();
        assertEquals(userId, passedToSave.getUserId());
        assertEquals(CredentialStatus.ISSUED.getCode(), passedToSave.getCredentialStatus());
        assertNull(passedToSave.getCredentialFormat());
        assertNull(passedToSave.getCredentialData());
        // plus any other checks you wish to make
    }

    @Test
    void testSaveCredential_JwtFormat_Success() throws Exception {
        // GIVEN
        String processId = "proc123";
        UUID userId = UUID.randomUUID();
        String credential = "someJwtData";
        // This is the 'CredentialResponse' with format=JWT_VC
        CredentialResponse response = CredentialResponse.builder()
                .format("jwt_vc")
                .credential(credential)
                .build();

        String credentialId = "8c7a6213-544d-450d-8e3d-b41fa9009198";

        // Suppose the repository will return a saved credential with a known ID
        Credential savedEntity = Credential.builder()
                .credentialId(UUID.fromString(credentialId))
                .userId(userId)
                .build();

        // We'll capture the 'Credential' passed into 'save(...)'
        ArgumentCaptor<Credential> captor = ArgumentCaptor.forClass(Credential.class);
        when(credentialRepository.save(captor.capture()))
                .thenReturn(Mono.just(savedEntity));

        SignedJWT mockJwt = mock(SignedJWT.class);

        // We'll define a payload that has "vc"
        String fakePayloadJson = """
        {
          "vc": {
            "id": "8c7a6213-544d-450d-8e3d-b41fa9009198",
            "type": ["VerifiableCredential", "LEARCredentialEmployee"]
          }
        }
        """;
        // Next, we create a 'Payload' object from the Nimbus library
        Payload mockPayload = new Payload(fakePayloadJson);

        // Make the SignedJWT return that payload
        when(mockJwt.getPayload()).thenReturn(mockPayload);

        // Use a static mock to intercept SignedJWT.parse(...)
        try (MockedStatic<SignedJWT> staticMock = Mockito.mockStatic(SignedJWT.class)) {
            // When someone calls SignedJWT.parse("someJwtData"), return mockJwt
            staticMock.when(() -> SignedJWT.parse(credential))
                    .thenReturn(mockJwt);

            // 2) We also need to mock objectMapper.readTree(...) so that it returns a root JsonNode
            //    representing fakePayloadJson
            JsonNode rootNode = new ObjectMapper().readTree(fakePayloadJson);
            when(objectMapper.readTree(fakePayloadJson)).thenReturn(rootNode);

            // WHEN
            Mono<UUID> result = credentialRepositoryService.saveCredential(processId, userId, response);

            // THEN
            StepVerifier.create(result)
                    .expectNext(UUID.fromString(credentialId))
                    .verifyComplete();

            verify(credentialRepository).save(any(Credential.class));

            Credential passedToSave = captor.getValue();
            assertEquals(CredentialStatus.VALID.getCode(), passedToSave.getCredentialStatus());
            assertEquals(userId, passedToSave.getUserId());
            assertEquals(CredentialFormats.JWT_VC.getCode(), passedToSave.getCredentialFormat());
            assertEquals(credential, passedToSave.getCredentialData());
        }
    }


    @Test
    void testSaveCredential_UnsupportedFormat_Error() {
        // GIVEN
        String processId = "proc123";
        UUID userId = UUID.randomUUID();
        CredentialResponse response = CredentialResponse.builder()
                .format("FOO_FORMAT")
                .credential("foo-data")
                .build();

        // WHEN
        Mono<UUID> result = credentialRepositoryService.saveCredential(processId, userId, response);

        // THEN
        StepVerifier.create(result)
                .expectErrorMatches(ex -> ex instanceof IllegalArgumentException
                        && ex.getMessage().contains("Unsupported credential format"))
                .verify();

        verify(credentialRepository, never()).save(any(Credential.class));
    }

    @Test
    void testSaveDeferredCredential_Success() {
        // We test a credential that is currently ISSUED => we can update to VALID
        String processId = "procXYZ";
        UUID userId = UUID.randomUUID();
        UUID credId = UUID.randomUUID();

        // The existing credential
        Credential existing = Credential.builder()
                .credentialId(credId)
                .userId(userId)
                .credentialStatus(CredentialStatus.ISSUED.getCode())
                .build();

        when(credentialRepository.findById(credId)).thenReturn(Mono.just(existing));

        // Suppose once we update the credential, we store it as VALID
        Credential updated = Credential.builder()
                .credentialId(credId)
                .userId(userId)
                .credentialStatus(CredentialStatus.VALID.getCode())
                .build();

        ArgumentCaptor<Credential> captor = ArgumentCaptor.forClass(Credential.class);
        when(credentialRepository.save(captor.capture())).thenReturn(Mono.just(updated));

        CredentialResponse deferredResponse = CredentialResponse.builder()
                .format("jwt_vc")
                .credential("some-jwt-data")
                .build();

        Mono<Void> result = credentialRepositoryService.saveDeferredCredential(
                processId,
                userId.toString(),
                credId.toString(),
                deferredResponse
        );

        StepVerifier.create(result).verifyComplete();

        verify(credentialRepository).save(any(Credential.class));

        // Check that the repository saved with status = VALID
        Credential passedToSave = captor.getValue();
        assertEquals(CredentialStatus.VALID.getCode(), passedToSave.getCredentialStatus());
        assertEquals("some-jwt-data", passedToSave.getCredentialData());
    }

    @Test
    void testExtractDidFromCredential_BasicType() throws JsonProcessingException {
        String processId = "procDid";
        UUID userUuid = UUID.randomUUID();
        UUID credUuid = UUID.randomUUID();
        String credential = "credential";

        // The credential has no LEARCredentialEmployee => DID is at /credentialSubject/id
        Credential existing = Credential.builder()
                .credentialId(credUuid)
                .userId(userUuid)
                .credentialType(List.of("VerifiableCredential"))
                .jsonVc(credential)
                .build();

        when(credentialRepository.findById(credUuid))
                .thenReturn(Mono.just(existing));

        when(objectMapper.readTree(credential)).thenReturn(getJsonNodeCredential());

        Mono<String> result =
                credentialRepositoryService.extractDidFromCredential(processId,
                        credUuid.toString(),
                        userUuid.toString()
                );

        StepVerifier.create(result)
                .expectNext("did:example:basic")
                .verifyComplete();
    }

    @Test
    void testExtractDidFromCredential_LearType() throws JsonProcessingException {
        String processId = "procDid";
        UUID userUuid = UUID.randomUUID();
        UUID credUuid = UUID.randomUUID();
        String credential = "credential";

        // The credential has type "LEARCredentialEmployee", so DID is at /credentialSubject/mandate/mandatee/id
        Credential existing = Credential.builder()
                .credentialId(credUuid)
                .userId(userUuid)
                .credentialType(List.of("VerifiableCredential", "LEARCredentialEmployee"))
                .jsonVc(credential)
                .build();

        when(credentialRepository.findById(credUuid)).thenReturn(Mono.just(existing));
        when(objectMapper.readTree(credential)).thenReturn(getJsonNodeCredentialLearCredentialEmployee());

        Mono<String> result =
                credentialRepositoryService.extractDidFromCredential(processId,
                        credUuid.toString(),
                        userUuid.toString()
                );

        StepVerifier.create(result)
                .expectNext("did:example:987")
                .verifyComplete();
    }

    @Test
    void testGetCredentialsByUserId_Success() {
        String processId = "procABC";
        UUID userUuid = UUID.randomUUID();

        // Suppose the repository returns 2 credentials for the user
        Credential c1 = Credential.builder()
                .credentialId(UUID.randomUUID())
                .userId(userUuid)
                .credentialType(List.of("VerifiableCredential", "SomeType"))
                .credentialStatus(CredentialStatus.VALID.getCode())
                .jsonVc("{\"credentialSubject\":{\"id\":\"did:example:123\"}}")
                .build();
        Credential c2 = Credential.builder()
                .credentialId(UUID.randomUUID())
                .userId(userUuid)
                .credentialType(List.of("VerifiableCredential", "AnotherType"))
                .credentialStatus(CredentialStatus.ISSUED.getCode())
                .jsonVc("{\"credentialSubject\":{\"id\":\"did:example:456\"}}")
                .build();

        when(credentialRepository.findAllByUserId(userUuid))
                .thenReturn(Flux.just(c1, c2));

        Mono<List<CredentialsBasicInfo>> result =
                credentialRepositoryService.getCredentialsByUserId(processId, userUuid.toString());

        StepVerifier.create(result)
                .assertNext(list -> {
                    assertEquals(2, list.size());
                })
                .verifyComplete();
    }

    @Test
    void testGetCredentialDataByIdAndUserId_Success() {
        String processId = "procDEF";
        UUID userUuid = UUID.randomUUID();
        UUID credUuid = UUID.randomUUID();

        Credential existing = Credential.builder()
                .credentialId(credUuid)
                .userId(userUuid)
                .credentialData("some-raw-data-here")
                .build();

        when(credentialRepository.findById(credUuid)).thenReturn(Mono.just(existing));

        Mono<String> result = credentialRepositoryService.getCredentialDataByIdAndUserId(
                processId,
                userUuid.toString(),
                credUuid.toString()
        );

        StepVerifier.create(result)
                .expectNext("some-raw-data-here")
                .verifyComplete();
    }

    @Test
    void testDeleteCredential_Success() {
        String processId = "procDel";
        UUID userUuid = UUID.randomUUID();
        UUID credUuid = UUID.randomUUID();

        Credential existing = Credential.builder()
                .credentialId(credUuid)
                .userId(userUuid)
                .build();

        when(credentialRepository.findById(credUuid))
                .thenReturn(Mono.just(existing));
        when(credentialRepository.delete(existing))
                .thenReturn(Mono.empty());

        Mono<Void> result = credentialRepositoryService.deleteCredential(
                processId,
                credUuid.toString(),
                userUuid.toString()
        );

        StepVerifier.create(result).verifyComplete();

        verify(credentialRepository).delete(existing);
    }

    private JsonNode getJsonNodeCredentialLearCredentialEmployee() throws JsonProcessingException {
        String json = """
                {
                    "id": "8c7a6213-544d-450d-8e3d-b41fa9009198",
                    "type": [
                        "VerifiableCredential",
                        "LEARCredentialEmployee"
                    ],
                    "credentialSubject" : {
                        "mandate" : {
                            "mandatee": {
                                "id": "did:example:987"
                                }
                            }
                        }
                     }
                }
                """;
        ObjectMapper objectMapper2 = new ObjectMapper();
        return objectMapper2.readTree(json);
    }

    private JsonNode getJsonNodeCredential() throws JsonProcessingException {
        String json = """
                {
                    "id": "8c7a6213-544d-450d-8e3d-b41fa9009198",
                    "type": [
                        "VerifiableCredential",
                        "LEARCredentialEmployee"
                    ],
                    "credentialSubject" : {
                        "id" : "did:example:basic"                     }
                }
                """;
        ObjectMapper objectMapper2 = new ObjectMapper();
        return objectMapper2.readTree(json);
    }
}


