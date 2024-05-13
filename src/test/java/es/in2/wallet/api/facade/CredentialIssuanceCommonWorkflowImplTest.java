package es.in2.wallet.api.facade;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.application.port.BrokerService;
import es.in2.wallet.application.workflow.issuance.impl.CredentialIssuanceCommonWorkflowImpl;
import es.in2.wallet.domain.model.*;
import es.in2.wallet.domain.service.*;
import es.in2.wallet.domain.util.ApplicationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuples;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static es.in2.wallet.domain.util.ApplicationConstants.*;
import static es.in2.wallet.domain.util.ApplicationUtils.extractResponseType;
import static es.in2.wallet.domain.util.ApplicationUtils.getUserIdFromToken;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialIssuanceCommonWorkflowImplTest {

    @Mock
    private CredentialOfferService credentialOfferService;
    @Mock
    private CredentialIssuerMetadataService credentialIssuerMetadataService;
    @Mock
    private AuthorisationServerMetadataService authorisationServerMetadataService;
    @Mock
    private PreAuthorizedService preAuthorizedService;
    @Mock
    private DidKeyGeneratorService didKeyGeneratorService;
    @Mock
    private CredentialService credentialService;
    @Mock
    private DataService dataService;
    @Mock
    private BrokerService brokerService;
    @Mock
    private ProofJWTService proofJWTService;
    @Mock
    private SignerService signerService;
    @Mock
    private EbsiAuthorisationService ebsiAuthorisationService;
    @Mock
    private EbsiIdTokenService ebsiIdTokenService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private EbsiVpTokenService ebsiVpTokenService;

    @InjectMocks
    private CredentialIssuanceCommonWorkflowImpl credentialIssuanceServiceFacade;

    @Test
    void getCredentialWithPreAuthorizedCode_UserEntityExists_UpdatesEntityWithCredential() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)) {
            String processId = "processId";
            String authorizationToken = "authToken";
            String qrContent = "qrContent";
            CredentialOffer.Credential credential = CredentialOffer.Credential.builder().format("jwt_vc").types(List.of("LEARCredential")).build();
            CredentialOffer.Grant grant = CredentialOffer.Grant.builder().preAuthorizedCodeGrant(CredentialOffer.Grant.PreAuthorizedCodeGrant.builder().build()).build();
            CredentialOffer credentialOffer = CredentialOffer.builder().grant(grant).credentials(List.of(credential)).build();
            AuthorisationServerMetadata authorisationServerMetadata = AuthorisationServerMetadata.builder().build();
            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").build();
            TokenResponse tokenResponse = TokenResponse.builder().cNonce("123").build();
            CredentialResponse credentialResponse = CredentialResponse.builder().credential("ey1234").build();
            String did = "did:ebsi:123";
            String userEntity = "existingUserEntity";
            String credentialEntity = "credentialEntity";
            String json = "{\"credential_request\":\"example\"}";
            ObjectMapper objectMapper2 = new ObjectMapper();
            JsonNode jsonNode = objectMapper2.readTree(json);
            String jwtProof = "jwt";

            when(getUserIdFromToken(authorizationToken)).thenReturn(Mono.just("userId"));
            when(credentialOfferService.getCredentialOfferFromCredentialOfferUri(processId, qrContent)).thenReturn(Mono.just(credentialOffer));
            when(credentialIssuerMetadataService.getCredentialIssuerMetadataFromCredentialOffer(processId, credentialOffer)).thenReturn(Mono.just(credentialIssuerMetadata));
            when(authorisationServerMetadataService.getAuthorizationServerMetadataFromCredentialIssuerMetadata(processId, credentialIssuerMetadata)).thenReturn(Mono.just(authorisationServerMetadata));
            when(didKeyGeneratorService.generateDidKey()).thenReturn(Mono.just(did));
            when(preAuthorizedService.getPreAuthorizedToken(processId, credentialOffer, authorisationServerMetadata, authorizationToken)).thenReturn(Mono.just(tokenResponse));
            when(proofJWTService.buildCredentialRequest(tokenResponse.cNonce(), credentialIssuerMetadata.credentialIssuer(), did)).thenReturn(Mono.just(jsonNode));
            when(signerService.buildJWTSFromJsonNode(jsonNode, did, "proof")).thenReturn(Mono.just(jwtProof));
            when(credentialService.getCredential(jwtProof, tokenResponse, credentialIssuerMetadata, credentialOffer.credentials().get(0).format(), credentialOffer.credentials().get(0).types())).thenReturn(Mono.just(credentialResponse));
            when(brokerService.getEntityById(processId, USER_ENTITY_PREFIX + "userId")).thenReturn(Mono.just(Optional.of(userEntity)));
            when(dataService.saveVC(processId,"userId", credentialResponse)).thenReturn(Mono.just(credentialEntity));
            when(brokerService.postEntity(processId, credentialEntity)).thenReturn(Mono.empty());

            StepVerifier.create(credentialIssuanceServiceFacade.identifyAuthMethod(processId, authorizationToken, qrContent)).verifyComplete();
        }
    }

    @Test
    void getCredentialWithPreAuthorizedCode_UserEntityNoExists_UpdatesEntityWithCredential() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)) {
            String processId = "processId";
            String authorizationToken = "authToken";
            String qrContent = "qrContent";
            CredentialOffer.Credential credential = CredentialOffer.Credential.builder().format("jwt_vc").types(List.of("LEARCredential")).build();
            CredentialOffer.Grant grant = CredentialOffer.Grant.builder().preAuthorizedCodeGrant(CredentialOffer.Grant.PreAuthorizedCodeGrant.builder().build()).build();
            CredentialOffer credentialOffer = CredentialOffer.builder().grant(grant).credentials(List.of(credential)).build();
            AuthorisationServerMetadata authorisationServerMetadata = AuthorisationServerMetadata.builder().build();
            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").build();
            TokenResponse tokenResponse = TokenResponse.builder().cNonce("123").build();
            CredentialResponse credentialResponse = CredentialResponse.builder().credential("ey1234").build();
            String did = "did:ebsi:123";
            String userEntity = "existingUserEntity";
            String credentialEntity = "credentialEntity";
            String json = "{\"credential_request\":\"example\"}";
            ObjectMapper objectMapper2 = new ObjectMapper();
            JsonNode jsonNode = objectMapper2.readTree(json);
            String jwtProof = "jwt";

            when(getUserIdFromToken(authorizationToken)).thenReturn(Mono.just("userId"));
            when(credentialOfferService.getCredentialOfferFromCredentialOfferUri(processId, qrContent)).thenReturn(Mono.just(credentialOffer));
            when(credentialIssuerMetadataService.getCredentialIssuerMetadataFromCredentialOffer(processId, credentialOffer)).thenReturn(Mono.just(credentialIssuerMetadata));
            when(authorisationServerMetadataService.getAuthorizationServerMetadataFromCredentialIssuerMetadata(processId, credentialIssuerMetadata)).thenReturn(Mono.just(authorisationServerMetadata));
            when(didKeyGeneratorService.generateDidKey()).thenReturn(Mono.just(did));
            when(preAuthorizedService.getPreAuthorizedToken(processId, credentialOffer, authorisationServerMetadata, authorizationToken)).thenReturn(Mono.just(tokenResponse));
            when(proofJWTService.buildCredentialRequest(tokenResponse.cNonce(), credentialIssuerMetadata.credentialIssuer(), did)).thenReturn(Mono.just(jsonNode));
            when(signerService.buildJWTSFromJsonNode(jsonNode, did, "proof")).thenReturn(Mono.just(jwtProof));
            when(credentialService.getCredential(jwtProof, tokenResponse, credentialIssuerMetadata, credentialOffer.credentials().get(0).format(), credentialOffer.credentials().get(0).types())).thenReturn(Mono.just(credentialResponse));
            when(brokerService.getEntityById(processId, USER_ENTITY_PREFIX + "userId")).thenReturn(Mono.just(Optional.empty())) //First interaction return empty because it's a new user
                    .thenReturn(Mono.just(Optional.of(userEntity)));
            when(dataService.createUserEntity("userId")).thenReturn(Mono.just("NewUserEntity"));
            when(brokerService.postEntity(processId, "NewUserEntity")).thenReturn(Mono.empty());
            when(dataService.saveVC(processId,"userId", credentialResponse)).thenReturn(Mono.just(credentialEntity));
            when(brokerService.postEntity(processId, credentialEntity)).thenReturn(Mono.empty());

            StepVerifier.create(credentialIssuanceServiceFacade.identifyAuthMethod(processId, authorizationToken, qrContent)).verifyComplete();
        }
    }

    @Test
    void getCredentialWithAuthorizedCodeEbsiIdToken_UserEntityExists_UpdatesEntityWithCredential() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)) {
            String processId = "processId";
            String authorizationToken = "authToken";
            String qrContent = "qrContent";
            CredentialOffer.Credential credential = CredentialOffer.Credential.builder().format("jwt_vc").types(List.of("LEARCredential")).build();
            CredentialOffer.Grant grant = CredentialOffer.Grant.builder().authorizationCodeGrant(CredentialOffer.Grant.AuthorizationCodeGrant.builder().build()).build();
            CredentialOffer credentialOffer = CredentialOffer.builder().grant(grant).credentials(List.of(credential)).build();
            AuthorisationServerMetadata authorisationServerMetadata = AuthorisationServerMetadata.builder().build();
            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").build();
            TokenResponse tokenResponse = TokenResponse.builder().cNonce("123").build();
            CredentialResponse credentialResponse = CredentialResponse.builder().credential("ey1234").build();
            String did = "did:ebsi:123";
            String userEntity = "existingUserEntity";
            String credentialEntity = "credentialEntity";
            String json = "{\"credential_request\":\"example\"}";
            ObjectMapper objectMapper2 = new ObjectMapper();
            JsonNode jsonNode = objectMapper2.readTree(json);
            String jwtProof = "jwt";
            Map<String, String> mockedMap = new HashMap<>();
            mockedMap.put("code", "123");

            when(getUserIdFromToken(authorizationToken)).thenReturn(Mono.just("userId"));
            when(credentialOfferService.getCredentialOfferFromCredentialOfferUri(processId, qrContent)).thenReturn(Mono.just(credentialOffer));
            when(credentialIssuerMetadataService.getCredentialIssuerMetadataFromCredentialOffer(processId, credentialOffer)).thenReturn(Mono.just(credentialIssuerMetadata));
            when(authorisationServerMetadataService.getAuthorizationServerMetadataFromCredentialIssuerMetadata(processId, credentialIssuerMetadata)).thenReturn(Mono.just(authorisationServerMetadata));
            when(didKeyGeneratorService.generateDidKey()).thenReturn(Mono.just(did));
            when(ebsiAuthorisationService.getRequestWithOurGeneratedCodeVerifier(processId, credentialOffer, authorisationServerMetadata, credentialIssuerMetadata, did)).thenReturn(Mono.just(Tuples.of("jwt", "codeVerifier")));
            when(extractResponseType("jwt")).thenReturn(Mono.just("id_token"));
            when(ebsiIdTokenService.getIdTokenResponse(processId, did, authorisationServerMetadata, "jwt")).thenReturn(Mono.just(mockedMap));
            when(ebsiAuthorisationService.sendTokenRequest("codeVerifier", did, authorisationServerMetadata, mockedMap)).thenReturn(Mono.just(tokenResponse));
            when(proofJWTService.buildCredentialRequest(tokenResponse.cNonce(), credentialIssuerMetadata.credentialIssuer(), did)).thenReturn(Mono.just(jsonNode));
            when(signerService.buildJWTSFromJsonNode(jsonNode, did, "proof")).thenReturn(Mono.just(jwtProof));
            when(credentialService.getCredential(jwtProof, tokenResponse, credentialIssuerMetadata, credentialOffer.credentials().get(0).format(), credentialOffer.credentials().get(0).types())).thenReturn(Mono.just(credentialResponse));
            when(brokerService.getEntityById(processId, USER_ENTITY_PREFIX + "userId")).thenReturn(Mono.just(Optional.of(userEntity)));
            when(dataService.saveVC(processId,"userId", credentialResponse)).thenReturn(Mono.just(credentialEntity));
            when(brokerService.postEntity(processId, credentialEntity)).thenReturn(Mono.empty());

            StepVerifier.create(credentialIssuanceServiceFacade.identifyAuthMethod(processId, authorizationToken, qrContent)).verifyComplete();
        }
    }

    @Test
    void getCredentialWithAuthorizedCodeEbsiVpToken_UserEntityExists_UpdatesEntityWithCredential() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)) {
            String processId = "processId";
            String authorizationToken = "authToken";
            String qrContent = "qrContent";
            CredentialOffer.Credential credential = CredentialOffer.Credential.builder().format("jwt_vc").types(List.of("LEARCredential")).build();
            CredentialOffer.Grant grant = CredentialOffer.Grant.builder().authorizationCodeGrant(CredentialOffer.Grant.AuthorizationCodeGrant.builder().build()).build();
            CredentialOffer credentialOffer = CredentialOffer.builder().grant(grant).credentials(List.of(credential)).build();
            AuthorisationServerMetadata authorisationServerMetadata = AuthorisationServerMetadata.builder().build();
            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").build();
            TokenResponse tokenResponse = TokenResponse.builder().cNonce("123").build();
            CredentialResponse credentialResponse = CredentialResponse.builder().credential("ey1234").build();
            String did = "did:ebsi:123";
            String userEntity = "existingUserEntity";
            String credentialEntity = "credentialEntity";
            String json = "{\"credential_request\":\"example\"}";
            ObjectMapper objectMapper2 = new ObjectMapper();
            JsonNode jsonNode = objectMapper2.readTree(json);
            String jwtProof = "jwt";
            Map<String, String> mockedMap = new HashMap<>();
            mockedMap.put("code", "123");

            when(getUserIdFromToken(authorizationToken)).thenReturn(Mono.just("userId"));
            when(credentialOfferService.getCredentialOfferFromCredentialOfferUri(processId, qrContent)).thenReturn(Mono.just(credentialOffer));
            when(credentialIssuerMetadataService.getCredentialIssuerMetadataFromCredentialOffer(processId, credentialOffer)).thenReturn(Mono.just(credentialIssuerMetadata));
            when(authorisationServerMetadataService.getAuthorizationServerMetadataFromCredentialIssuerMetadata(processId, credentialIssuerMetadata)).thenReturn(Mono.just(authorisationServerMetadata));
            when(didKeyGeneratorService.generateDidKey()).thenReturn(Mono.just(did));
            when(ebsiAuthorisationService.getRequestWithOurGeneratedCodeVerifier(processId, credentialOffer, authorisationServerMetadata, credentialIssuerMetadata, did)).thenReturn(Mono.just(Tuples.of("jwt", "codeVerifier")));
            when(extractResponseType("jwt")).thenReturn(Mono.just("vp_token"));
            when(ebsiVpTokenService.getVpRequest(processId, authorizationToken, authorisationServerMetadata, "jwt")).thenReturn(Mono.just(mockedMap));
            when(ebsiAuthorisationService.sendTokenRequest("codeVerifier", did, authorisationServerMetadata, mockedMap)).thenReturn(Mono.just(tokenResponse));
            when(proofJWTService.buildCredentialRequest(tokenResponse.cNonce(), credentialIssuerMetadata.credentialIssuer(), did)).thenReturn(Mono.just(jsonNode));
            when(signerService.buildJWTSFromJsonNode(jsonNode, did, "proof")).thenReturn(Mono.just(jwtProof));
            when(credentialService.getCredential(jwtProof, tokenResponse, credentialIssuerMetadata, credentialOffer.credentials().get(0).format(), credentialOffer.credentials().get(0).types())).thenReturn(Mono.just(credentialResponse));
            when(brokerService.getEntityById(processId, USER_ENTITY_PREFIX + "userId")).thenReturn(Mono.just(Optional.of(userEntity)));
            when(dataService.saveVC(processId,"userId", credentialResponse)).thenReturn(Mono.just(credentialEntity));
            when(brokerService.postEntity(processId, credentialEntity)).thenReturn(Mono.empty());

            StepVerifier.create(credentialIssuanceServiceFacade.identifyAuthMethod(processId, authorizationToken, qrContent)).verifyComplete();
        }
    }

    @Test
    void getCredentialWithPreAuthorizedCodeDOMEProfile_UserEntityExists_UpdatesEntityWithCredential() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)) {
            String processId = "processId";
            String authorizationToken = "authToken";
            String qrContent = "qrContent";
            CredentialOffer.Grant grant = CredentialOffer.Grant.builder().preAuthorizedCodeGrant(CredentialOffer.Grant.PreAuthorizedCodeGrant.builder().build()).build();
            CredentialOffer credentialOffer = CredentialOffer.builder().grant(grant).credentialConfigurationsIds(List.of("LEARCredential")).build();
            AuthorisationServerMetadata authorisationServerMetadata = AuthorisationServerMetadata.builder().build();

            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder()
                    .credentialsConfigurationsSupported(Map.of("LEARCredential",
                            CredentialIssuerMetadata.CredentialsConfigurationsSupported.builder().format(JWT_VC).build()))
                    .credentialIssuer("issuer")
                    .deferredCredentialEndpoint("https://example.com/deferred")
                    .build();

            TokenResponse tokenResponse = TokenResponse.builder().cNonce("123").build();
            CredentialResponse credentialResponse = CredentialResponse.builder().credential("unsigned_credential").format(VC_JSON).transactionId("123").build();
            String did = "did:ebsi:123";
            String userEntity = "existingUserEntity";
            String credentialEntity = "credentialEntity";
            String json = "{\"credential_request\":\"example\"}";
            ObjectMapper objectMapper2 = new ObjectMapper();
            JsonNode jsonNode = objectMapper2.readTree(json);
            String jwtProof = "jwt";

            String jsonCredential = """
                        {
                            "type": [
                              "VerifiableCredential",
                              "LEARCredential"
                            ],
                            "@context": [
                              "https://www.w3.org/2018/credentials/v1",
                              "https://dome-marketplace.eu//2022/credentials/learcredential/v1"
                            ],
                            "id": "urn:uuid:8700bee5-4621-4720-94de-e866fb7197e9",
                            "issuer": "did:key:zQ3shtcEQP3yuxbkZ1SjN51T8fQmRyXnjrXm8E84WLXKDQbRn",
                            "issuanceDate": "2023-10-24T08:07:35Z",
                            "issued": "2023-10-24T08:07:35Z",
                            "validFrom": "2023-10-24T08:07:35Z",
                            "expirationDate": "2023-11-23T08:07:35Z",
                            "credentialSubject": {
                              "id": "did:example:123",
                              "title": "Mr.",
                              "first_name": "John",
                              "last_name": "Doe",
                              "gender": "M",
                              "postal_address": "",
                              "email": "johndoe@goodair.com",
                              "telephone": "",
                              "fax": "",
                              "mobile_phone": "+34787426623",
                              "legalRepresentative": {
                                "cn": "56565656V Jesus Ruiz",
                                "serialNumber": "56565656V",
                                "organizationIdentifier": "VATES-12345678",
                                "o": "GoodAir",
                                "c": "ES"
                              },
                              "rolesAndDuties": [
                                {
                                  "type": "LEARCredential",
                                  "id": "https://dome-marketplace.eu//lear/v1/6484994n4r9e990494"
                                }
                              ]
                            }
                          }
                """;
            JsonNode jsonNodeCredential = objectMapper2.readTree(jsonCredential);

            when(getUserIdFromToken(authorizationToken)).thenReturn(Mono.just("userId"));
            when(credentialOfferService.getCredentialOfferFromCredentialOfferUri(processId, qrContent)).thenReturn(Mono.just(credentialOffer));
            when(credentialIssuerMetadataService.getCredentialIssuerMetadataFromCredentialOffer(processId, credentialOffer)).thenReturn(Mono.just(credentialIssuerMetadata));
            when(authorisationServerMetadataService.getAuthorizationServerMetadataFromCredentialIssuerMetadata(processId, credentialIssuerMetadata)).thenReturn(Mono.just(authorisationServerMetadata));
            when(didKeyGeneratorService.generateDidKey()).thenReturn(Mono.just(did));
            when(preAuthorizedService.getPreAuthorizedToken(processId, credentialOffer, authorisationServerMetadata, authorizationToken)).thenReturn(Mono.just(tokenResponse));
            when(proofJWTService.buildCredentialRequest(tokenResponse.cNonce(), credentialIssuerMetadata.credentialIssuer(), did)).thenReturn(Mono.just(jsonNode));
            when(signerService.buildJWTSFromJsonNode(jsonNode, did, "proof")).thenReturn(Mono.just(jwtProof));
            when(credentialService.getCredential(jwtProof, tokenResponse, credentialIssuerMetadata, JWT_VC, null)).thenReturn(Mono.just(credentialResponse));
            when(brokerService.getEntityById(processId, USER_ENTITY_PREFIX + "userId")).thenReturn(Mono.just(Optional.of(userEntity)));
            when(dataService.saveDOMEUnsignedCredential("userId", credentialResponse.credential())).thenReturn(Mono.just(credentialEntity));
            when(brokerService.postEntity(processId, credentialEntity)).thenReturn(Mono.empty());
            when(objectMapper.readTree(anyString())).thenReturn(jsonNodeCredential);
            when(dataService.saveTransaction(
                    "urn:uuid:8700bee5-4621-4720-94de-e866fb7197e9",
                    credentialResponse.transactionId(),
                    tokenResponse.accessToken(),
                    credentialIssuerMetadata.deferredCredentialEndpoint()))
                    .thenReturn(Mono.just("transaction entity"));
            when(brokerService.postEntity(processId, "transaction entity")).thenReturn(Mono.empty());

            StepVerifier.create(credentialIssuanceServiceFacade.identifyAuthMethod(processId, authorizationToken, qrContent)).verifyComplete();
        }
    }
}

