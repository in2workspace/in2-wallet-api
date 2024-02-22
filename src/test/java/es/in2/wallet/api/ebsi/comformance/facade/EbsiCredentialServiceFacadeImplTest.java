//package es.in2.wallet.api.ebsi.comformance.facade;
//
//import es.in2.wallet.api.ebsi.comformance.config.EbsiConfig;
//import es.in2.wallet.api.ebsi.comformance.facade.impl.EbsiCredentialServiceFacadeImpl;
//import es.in2.wallet.api.model.*;
//import es.in2.wallet.api.service.*;
//import es.in2.wallet.api.util.ApplicationUtils;
//import es.in2.wallet.broker.service.BrokerService;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockedStatic;
//import org.mockito.Mockito;
//import org.mockito.junit.jupiter.MockitoExtension;
//import reactor.core.publisher.Mono;
//import reactor.test.StepVerifier;
//
//import java.util.List;
//import java.util.Optional;
//
//import static es.in2.wallet.api.util.ApplicationUtils.extractResponseType;
//import static es.in2.wallet.api.util.ApplicationUtils.getUserIdFromToken;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//class EbsiCredentialServiceFacadeImplTest {
//
//    @Mock
//    private CredentialOfferService credentialOfferService;
//    @Mock
//    private EbsiConfig ebsiConfig;
//    @Mock
//    private CredentialIssuerMetadataService credentialIssuerMetadataService;
//    @Mock
//    private AuthorisationServerMetadataService authorisationServerMetadataService;
//    @Mock
//    private PreAuthorizedService preAuthorizedService;
//    @Mock
//    private CredentialEbsiService credentialEbsiService;
//    @Mock
//    private UserDataService userDataService;
//    @Mock
//    private BrokerService brokerService;
//
//    @InjectMocks
//    private EbsiCredentialServiceFacadeImpl ebsiCredentialServiceFacade;
//
//    @Test
//    void getCredentialWithPreAuthorizedCodeEbsi_UserEntityExists_UpdatesEntityWithCredential() {
//        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)){
//            String processId = "processId";
//            String authorizationToken = "authToken";
//            String qrContent = "qrContent";
//            CredentialOffer.Credential credential = CredentialOffer.Credential.builder().format("jwt_vc").types(List.of("LEARCredential")).build();
//            CredentialOffer.Grant grant = CredentialOffer.Grant.builder().preAuthorizedCodeGrant(CredentialOffer.Grant.PreAuthorizedCodeGrant.builder().build()).build();
//            CredentialOffer credentialOffer = CredentialOffer.builder().grant(grant).credentials(List.of(credential)).build();
//            AuthorisationServerMetadata authorisationServerMetadata = AuthorisationServerMetadata.builder().build();
//            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().build();
//            TokenResponse tokenResponse = TokenResponse.builder().build();
//            CredentialResponse credentialResponse = CredentialResponse.builder().build();
//            String did = "did:ebsi:123";
//            String userEntity = "existingUserEntity";
//
//            when(extractResponseType(anyString())).thenReturn(Mono.just("id_token"));
//            when(getUserIdFromToken(authorizationToken)).thenReturn(Mono.just("userId"));
//            when(credentialOfferService.getCredentialOfferFromCredentialOfferUri(processId, qrContent)).thenReturn(Mono.just(credentialOffer));
//            when(credentialIssuerMetadataService.getCredentialIssuerMetadataFromCredentialOffer(processId,credentialOffer)).thenReturn(Mono.just(credentialIssuerMetadata));
//            when(authorisationServerMetadataService.getAuthorizationServerMetadataFromCredentialIssuerMetadata(processId,credentialIssuerMetadata)).thenReturn(Mono.just(authorisationServerMetadata));
//            when(ebsiConfig.getDid()).thenReturn(Mono.just(did));
//            when(preAuthorizedService.getPreAuthorizedToken(processId, credentialOffer, authorisationServerMetadata, authorizationToken)).thenReturn(Mono.just(tokenResponse));
//            when(credentialEbsiService.getCredential(processId, did, tokenResponse, credentialIssuerMetadata, credentialOffer.credentials().get(0).format(), credentialOffer.credentials().get(0).types())).thenReturn(Mono.just(credentialResponse));
//            when(brokerService.getEntityById(processId, "userId")).thenReturn(Mono.just(Optional.of(userEntity)));
//            when(userDataService.saveVC(userEntity, credentialResponse.credential())).thenReturn(Mono.just(userEntity));
//            when(brokerService.updateEntity(processId, "userId", userEntity)).thenReturn(Mono.empty());
//
//            StepVerifier.create(ebsiCredentialServiceFacade.identifyAuthMethod(processId, authorizationToken, qrContent))
//                    .verifyComplete();
//        }
//    }
//
//}
//
