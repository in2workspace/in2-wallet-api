package es.in2.wallet.api.facade;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.application.dto.*;
import es.in2.wallet.application.workflows.issuance.impl.CredentialIssuanceCommonWorkflowImpl;
import es.in2.wallet.domain.services.*;
import es.in2.wallet.domain.utils.ApplicationUtils;
import es.in2.wallet.infrastructure.services.CredentialRepositoryService;
import es.in2.wallet.infrastructure.services.DeferredCredentialMetadataRepositoryService;
import es.in2.wallet.infrastructure.services.UserRepositoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuples;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static es.in2.wallet.domain.utils.ApplicationConstants.JWT_VC;
import static es.in2.wallet.domain.utils.ApplicationUtils.extractResponseType;
import static es.in2.wallet.domain.utils.ApplicationUtils.getUserIdFromToken;
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
    private ProofJWTService proofJWTService;
    @Mock
    private SignerService signerService;
    @Mock
    private EbsiAuthorisationService ebsiAuthorisationService;
    @Mock
    private EbsiIdTokenService ebsiIdTokenService;
    @Mock
    private EbsiVpTokenService ebsiVpTokenService;
    @Mock
    private CredentialRepositoryService credentialRepositoryService;
    @Mock
    private UserRepositoryService userRepositoryService;
    @Mock
    private DeferredCredentialMetadataRepositoryService deferredCredentialMetadataRepositoryService;

    @InjectMocks
    private CredentialIssuanceCommonWorkflowImpl credentialIssuanceServiceFacade;

    @Test
    void getCredentialWithPreAuthorizedCode() throws JsonProcessingException {
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
            CredentialResponse credentialResponse = CredentialResponse.builder().credential("ey1234").format(JWT_VC).build();
            CredentialResponseWithStatus credentialResponseWithStatus = CredentialResponseWithStatus.builder().statusCode(HttpStatus.OK).credentialResponse(credentialResponse).build();
            String did = "did:ebsi:123";
            String json = "{\"credential_request\":\"example\"}";
            ObjectMapper objectMapper2 = new ObjectMapper();
            JsonNode jsonNode = objectMapper2.readTree(json);
            String jwtProof = "jwt";
            String userIdStr = UUID.randomUUID().toString();
            UUID userUuid = UUID.fromString(userIdStr);
            UUID credentialId = UUID.randomUUID();

            when(getUserIdFromToken(authorizationToken)).thenReturn(Mono.just(userIdStr));
            when(credentialOfferService.getCredentialOfferFromCredentialOfferUri(processId, qrContent)).thenReturn(Mono.just(credentialOffer));
            when(credentialIssuerMetadataService.getCredentialIssuerMetadataFromCredentialOffer(processId, credentialOffer)).thenReturn(Mono.just(credentialIssuerMetadata));
            when(authorisationServerMetadataService.getAuthorizationServerMetadataFromCredentialIssuerMetadata(processId, credentialIssuerMetadata)).thenReturn(Mono.just(authorisationServerMetadata));
            when(didKeyGeneratorService.generateDidKey()).thenReturn(Mono.just(did));
            when(preAuthorizedService.getPreAuthorizedToken(processId, credentialOffer, authorisationServerMetadata, authorizationToken)).thenReturn(Mono.just(tokenResponse));
            when(proofJWTService.buildCredentialRequest(tokenResponse.cNonce(), credentialIssuerMetadata.credentialIssuer(), did)).thenReturn(Mono.just(jsonNode));
            when(signerService.buildJWTSFromJsonNode(jsonNode, did, "proof")).thenReturn(Mono.just(jwtProof));
            when(credentialService.getCredential(jwtProof, tokenResponse, credentialIssuerMetadata, credentialOffer.credentials().get(0).format(), credentialOffer.credentials().get(0).types())).thenReturn(Mono.just(credentialResponseWithStatus));
            when(userRepositoryService.storeUser(processId, userIdStr)).thenReturn(Mono.just(userUuid));
            when(credentialRepositoryService.saveCredential(processId, userUuid, credentialResponse)).thenReturn(Mono.just(credentialId));

            StepVerifier.create(credentialIssuanceServiceFacade.identifyAuthMethod(processId, authorizationToken, qrContent)).verifyComplete();
        }
    }

    @Test
    void getCredentialWithAuthorizedCodeEbsiIdToken() throws JsonProcessingException {
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
            CredentialResponse credentialResponse = CredentialResponse.builder().credential("ey1234").format(JWT_VC).build();
            CredentialResponseWithStatus credentialResponseWithStatus = CredentialResponseWithStatus.builder().statusCode(HttpStatus.OK).credentialResponse(credentialResponse).build();
            String did = "did:ebsi:123";
            String json = "{\"credential_request\":\"example\"}";
            ObjectMapper objectMapper2 = new ObjectMapper();
            JsonNode jsonNode = objectMapper2.readTree(json);
            String jwtProof = "jwt";
            Map<String, String> mockedMap = new HashMap<>();
            mockedMap.put("code", "123");
            String userIdStr = UUID.randomUUID().toString();
            UUID userUuid = UUID.fromString(userIdStr);
            UUID credentialId = UUID.randomUUID();

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
            when(credentialService.getCredential(jwtProof, tokenResponse, credentialIssuerMetadata, credentialOffer.credentials().get(0).format(), credentialOffer.credentials().get(0).types())).thenReturn(Mono.just(credentialResponseWithStatus));
            when(userRepositoryService.storeUser(processId, userIdStr)).thenReturn(Mono.just(userUuid));
            when(credentialRepositoryService.saveCredential(processId, userUuid, credentialResponse)).thenReturn(Mono.just(credentialId));


            StepVerifier.create(credentialIssuanceServiceFacade.identifyAuthMethod(processId, authorizationToken, qrContent)).verifyComplete();
        }
    }


    @Test
    void getCredentialWithPreAuthorizedCodeDOMEProfile() throws JsonProcessingException {
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

            TokenResponse tokenResponse = TokenResponse.builder().cNonce("123").accessToken("ey1234").build();
            CredentialResponse credentialResponse = CredentialResponse.builder().credential("unsigned_credential").transactionId("123").build();
            CredentialResponseWithStatus credentialResponseWithStatus = CredentialResponseWithStatus.builder().statusCode(HttpStatus.ACCEPTED).credentialResponse(credentialResponse).build();
            String did = "did:ebsi:123";
            String json = "{\"credential_request\":\"example\"}";
            ObjectMapper objectMapper2 = new ObjectMapper();
            JsonNode jsonNode = objectMapper2.readTree(json);
            String jwtProof = "jwt";

            String userIdStr = UUID.randomUUID().toString();
            UUID userUuid = UUID.fromString(userIdStr);
            UUID credentialId = UUID.randomUUID();

            when(getUserIdFromToken(authorizationToken)).thenReturn(Mono.just(userIdStr));
            when(credentialOfferService.getCredentialOfferFromCredentialOfferUri(processId, qrContent)).thenReturn(Mono.just(credentialOffer));
            when(credentialIssuerMetadataService.getCredentialIssuerMetadataFromCredentialOffer(processId, credentialOffer)).thenReturn(Mono.just(credentialIssuerMetadata));
            when(authorisationServerMetadataService.getAuthorizationServerMetadataFromCredentialIssuerMetadata(processId, credentialIssuerMetadata)).thenReturn(Mono.just(authorisationServerMetadata));
            when(didKeyGeneratorService.generateDidKey()).thenReturn(Mono.just(did));
            when(preAuthorizedService.getPreAuthorizedToken(processId, credentialOffer, authorisationServerMetadata, authorizationToken)).thenReturn(Mono.just(tokenResponse));
            when(proofJWTService.buildCredentialRequest(tokenResponse.cNonce(), credentialIssuerMetadata.credentialIssuer(), did)).thenReturn(Mono.just(jsonNode));
            when(signerService.buildJWTSFromJsonNode(jsonNode, did, "proof")).thenReturn(Mono.just(jwtProof));
            when(credentialService.getCredential(jwtProof, tokenResponse, credentialIssuerMetadata, JWT_VC, null)).thenReturn(Mono.just(credentialResponseWithStatus));
            when(userRepositoryService.storeUser(processId, userIdStr)).thenReturn(Mono.just(userUuid));
            when(credentialRepositoryService.saveCredential(processId, userUuid, credentialResponse)).thenReturn(Mono.just(credentialId));
            when(deferredCredentialMetadataRepositoryService.saveDeferredCredentialMetadata(processId, credentialId, credentialResponse.transactionId(), tokenResponse.accessToken(), credentialIssuerMetadata.deferredCredentialEndpoint())).thenReturn(Mono.empty());

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
            CredentialResponseWithStatus credentialResponseWithStatus = CredentialResponseWithStatus.builder().statusCode(HttpStatus.OK).credentialResponse(credentialResponse).build();
            String did = "did:ebsi:123";
            String json = "{\"credential_request\":\"example\"}";
            ObjectMapper objectMapper2 = new ObjectMapper();
            JsonNode jsonNode = objectMapper2.readTree(json);
            String jwtProof = "jwt";
            Map<String, String> mockedMap = new HashMap<>();
            mockedMap.put("code", "123");

            String userIdStr = UUID.randomUUID().toString();
            UUID userUuid = UUID.fromString(userIdStr);
            UUID credentialId = UUID.randomUUID();

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
            when(credentialService.getCredential(jwtProof, tokenResponse, credentialIssuerMetadata, credentialOffer.credentials().get(0).format(), credentialOffer.credentials().get(0).types())).thenReturn(Mono.just(credentialResponseWithStatus));
            when(userRepositoryService.storeUser(processId, userIdStr)).thenReturn(Mono.just(userUuid));
            when(credentialRepositoryService.saveCredential(processId, userUuid, credentialResponse)).thenReturn(Mono.just(credentialId));

            StepVerifier.create(credentialIssuanceServiceFacade.identifyAuthMethod(processId, authorizationToken, qrContent)).verifyComplete();
        }
    }
}

