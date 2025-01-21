//package es.in2.wallet.api.facade;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import es.in2.wallet.application.ports.BrokerService;
//import es.in2.wallet.application.workflows.presentation.impl.AttestationExchangeCommonWorkflowImpl;
//import es.in2.wallet.application.dto.AuthorizationRequestOIDC4VP;
//import es.in2.wallet.application.dto.CredentialsBasicInfo;
//import es.in2.wallet.application.dto.VcSelectorRequest;
//import es.in2.wallet.application.dto.VcSelectorResponse;
//import es.in2.wallet.domain.services.*;
//import es.in2.wallet.domain.utils.ApplicationUtils;
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
//
//import static es.in2.wallet.domain.utils.ApplicationUtils.getUserIdFromToken;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//class AttestationExchangeCommonWorkflowImplTest {
//    @Mock
//    private AuthorizationRequestService authorizationRequestService;
//    @Mock
//    private AuthorizationResponseService authorizationResponseService;
//    @Mock
//    private VerifierValidationService verifierValidationService;
//    @Mock
//    private DataService dataService;
//    @Mock
//    private BrokerService brokerService;
//    @Mock
//    private PresentationService presentationService;
//    @InjectMocks
//    private AttestationExchangeCommonWorkflowImpl attestationExchangeServiceFacade;
//
//    @Test
//    void getSelectableCredentialsRequiredToBuildThePresentationTest() {
//        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)){
//            String processId = "123";
//            String authorizationToken = "authToken";
//            String qrContent = "qrContent";
//            String jwtAuthorizationRequest = "authRequest";
//            AuthorizationRequestOIDC4VP authorizationRequestOIDC4VP = AuthorizationRequestOIDC4VP.builder().scope(List.of("scope1")).responseUri("responseUri").state("state").build();
//            CredentialsBasicInfo credentialsBasicInfo = CredentialsBasicInfo.builder().build();
//            VcSelectorRequest expectedVcSelectorRequest = VcSelectorRequest.builder().redirectUri("responseUri").state("state").selectableVcList(List.of(credentialsBasicInfo)).build();
//            when(authorizationRequestService.getJwtRequestObjectFromUri(processId, qrContent)).thenReturn(Mono.just(jwtAuthorizationRequest));
//            when(verifierValidationService.verifyIssuerOfTheAuthorizationRequest(processId, jwtAuthorizationRequest)).thenReturn(Mono.just(jwtAuthorizationRequest));
//            when(authorizationRequestService.getAuthorizationRequestFromJwtAuthorizationRequestJWT(processId, jwtAuthorizationRequest)).thenReturn(Mono.just(authorizationRequestOIDC4VP));
//            when(getUserIdFromToken(authorizationToken)).thenReturn(Mono.just("userId"));
//            when(brokerService.getCredentialByCredentialTypeAndUserId(processId, authorizationRequestOIDC4VP.scope().get(0),"userId")).thenReturn(Mono.just("credentialEntity"));
//            when(dataService.getUserVCsInJson("credentialEntity")).thenReturn(Mono.just(List.of(credentialsBasicInfo)));
//
//            StepVerifier.create(attestationExchangeServiceFacade.processAuthorizationRequest(processId, authorizationToken, qrContent))
//                    .expectNext(expectedVcSelectorRequest)
//                    .verifyComplete();
//        }
//    }
//
//    @Test
//    void buildVerifiablePresentationWithSelectedVCsTest() throws JsonProcessingException {
//        String processId = "123";
//        String authorizationToken = "authToken";
//        String nonce = "321";
//        VcSelectorResponse vcSelectorResponse = VcSelectorResponse.builder().nonce(nonce).build();
//        String verifiablePresentation = "vp";
//
//        when(presentationService.createSignedVerifiablePresentation(
//                processId,
//                authorizationToken,
//                vcSelectorResponse,
//                nonce,
//                "https://self-issued.me/v2"
//        )).thenReturn(Mono.just(verifiablePresentation));
//        when(authorizationResponseService.buildAndPostAuthorizationResponseWithVerifiablePresentation(processId, vcSelectorResponse, verifiablePresentation, authorizationToken)).thenReturn(Mono.empty());
//
//        StepVerifier.create(attestationExchangeServiceFacade.buildVerifiablePresentationWithSelectedVCs(processId, authorizationToken, vcSelectorResponse))
//                .verifyComplete();
//    }
//
//}
