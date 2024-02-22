package es.in2.wallet.api.ebsi.comformance.facade;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.api.ebsi.comformance.config.EbsiConfig;
import es.in2.wallet.api.ebsi.comformance.facade.impl.EbsiCredentialServiceFacadeImpl;
import es.in2.wallet.api.model.*;
import es.in2.wallet.api.service.*;
import es.in2.wallet.api.util.ApplicationUtils;
import es.in2.wallet.broker.service.BrokerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;

import static es.in2.wallet.api.util.ApplicationUtils.extractResponseType;
import static es.in2.wallet.api.util.ApplicationUtils.getUserIdFromToken;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EbsiCredentialServiceFacadeImplTest {

    @Mock
    private CredentialOfferService credentialOfferService;
    @Mock
    private EbsiConfig ebsiConfig;
    @Mock
    private CredentialIssuerMetadataService credentialIssuerMetadataService;
    @Mock
    private AuthorisationServerMetadataService authorisationServerMetadataService;
    @Mock
    private PreAuthorizedService preAuthorizedService;
    @Mock
    private CredentialService credentialService;
    @Mock
    private UserDataService userDataService;
    @Mock
    private BrokerService brokerService;

    @Mock
    private ProofJWTService proofJWTService;
    @Mock
    private SignerService signerService;

    @InjectMocks
    private EbsiCredentialServiceFacadeImpl ebsiCredentialServiceFacade;

    @Test
    void getCredentialWithPreAuthorizedCodeEbsi_UserEntityExists_UpdatesEntityWithCredential() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)){
            String processId = "processId";
            String authorizationToken = "authToken";
            String qrContent = "qrContent";
            CredentialOffer.Credential credential = CredentialOffer.Credential.builder().format("jwt_vc").types(List.of("LEARCredential")).build();
            CredentialOffer.Grant grant = CredentialOffer.Grant.builder().preAuthorizedCodeGrant(CredentialOffer.Grant.PreAuthorizedCodeGrant.builder().build()).build();
            CredentialOffer credentialOffer = CredentialOffer.builder().grant(grant).credentials(List.of(credential)).build();
            AuthorisationServerMetadata authorisationServerMetadata = AuthorisationServerMetadata.builder().build();
            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().credentialIssuer("issuer").build();
            TokenResponse tokenResponse = TokenResponse.builder().cNonce("123").build();
            CredentialResponse credentialResponse = CredentialResponse.builder().build();
            String did = "did:ebsi:123";
            String userEntity = "existingUserEntity";
            String json = "{\"credential_request\":\"example\"}";
            ObjectMapper objectMapper2 = new ObjectMapper();
            JsonNode jsonNode = objectMapper2.readTree(json);
            String jwtProof = "jwt";

            when(extractResponseType(anyString())).thenReturn(Mono.just("id_token"));
            when(getUserIdFromToken(authorizationToken)).thenReturn(Mono.just("userId"));
            when(credentialOfferService.getCredentialOfferFromCredentialOfferUri(processId, qrContent)).thenReturn(Mono.just(credentialOffer));
            when(credentialIssuerMetadataService.getCredentialIssuerMetadataFromCredentialOffer(processId,credentialOffer)).thenReturn(Mono.just(credentialIssuerMetadata));
            when(authorisationServerMetadataService.getAuthorizationServerMetadataFromCredentialIssuerMetadata(processId,credentialIssuerMetadata)).thenReturn(Mono.just(authorisationServerMetadata));
            when(ebsiConfig.getDid()).thenReturn(Mono.just(did));
            when(preAuthorizedService.getPreAuthorizedToken(processId, credentialOffer, authorisationServerMetadata, authorizationToken)).thenReturn(Mono.just(tokenResponse));
            when(proofJWTService.buildCredentialRequest(tokenResponse.cNonce(),credentialIssuerMetadata.credentialIssuer(),did)).thenReturn(Mono.just(jsonNode));
            when(signerService.buildJWTSFromJsonNode(jsonNode,did,"proof")).thenReturn(Mono.just(jwtProof));
            when(credentialService.getCredential(processId, jwtProof, tokenResponse, credentialIssuerMetadata, credentialOffer.credentials().get(0).format(), credentialOffer.credentials().get(0).types())).thenReturn(Mono.just(credentialResponse));
            when(brokerService.getEntityById(processId, "userId")).thenReturn(Mono.just(Optional.of(userEntity)));
            when(userDataService.saveVC(userEntity, List.of(credentialResponse))).thenReturn(Mono.just(userEntity));
            when(brokerService.updateEntity(processId, "userId", userEntity)).thenReturn(Mono.empty());

            StepVerifier.create(ebsiCredentialServiceFacade.identifyAuthMethod(processId, authorizationToken, qrContent))
                    .verifyComplete();
        }
    }

}

