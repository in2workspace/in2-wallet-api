package es.in2.wallet.api.facade;

import com.fasterxml.jackson.core.JsonProcessingException;
import es.in2.wallet.api.facade.impl.AttestationExchangeServiceFacadeImpl;
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

import static es.in2.wallet.api.util.ApplicationUtils.getUserIdFromToken;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttestationExchangeServiceFacadeImplTest {
    @Mock
    private AuthorizationRequestService authorizationRequestService;
    @Mock
    private AuthorizationResponseService authorizationResponseService;
    @Mock
    private VerifierValidationService verifierValidationService;
    @Mock
    private UserDataService userDataService;
    @Mock
    private BrokerService brokerService;
    @Mock
    private PresentationService presentationService;
    @InjectMocks
    private AttestationExchangeServiceFacadeImpl attestationExchangeServiceFacade;

    @Test
    void getSelectableCredentialsRequiredToBuildThePresentationTest() {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)){
            String processId = "123";
            String authorizationToken = "authToken";
            String qrContent = "qrContent";
            String jwtAuthorizationRequest = "authRequest";
            AuthorizationRequest authorizationRequest = AuthorizationRequest.builder().scope(List.of("scope1")).redirectUri("redirectUri").state("state").build();
            String userEntity = "existingUserEntity";
            CredentialsBasicInfo credentialsBasicInfo = CredentialsBasicInfo.builder().build();
            VcSelectorRequest expectedVcSelectorRequest = VcSelectorRequest.builder().redirectUri("redirectUri").state("state").selectableVcList(List.of(credentialsBasicInfo)).build();
            when(authorizationRequestService.getAuthorizationRequestFromVcLoginRequest(processId, qrContent, authorizationToken)).thenReturn(Mono.just(jwtAuthorizationRequest));
            when(verifierValidationService.verifyIssuerOfTheAuthorizationRequest(processId, jwtAuthorizationRequest)).thenReturn(Mono.just(jwtAuthorizationRequest));
            when(authorizationRequestService.getAuthorizationRequestFromJwtAuthorizationRequestClaim(processId, jwtAuthorizationRequest)).thenReturn(Mono.just(authorizationRequest));
            when(getUserIdFromToken(authorizationToken)).thenReturn(Mono.just("userId"));
            when(brokerService.getEntityById(processId, "userId")).thenReturn(Mono.just(Optional.of(userEntity)));
            when(userDataService.getSelectableVCsByVcTypeList(List.of("scope1"), userEntity)).thenReturn(Mono.just(List.of(credentialsBasicInfo)));

            StepVerifier.create(attestationExchangeServiceFacade.getSelectableCredentialsRequiredToBuildThePresentation(processId, authorizationToken, qrContent))
                    .expectNext(expectedVcSelectorRequest)
                    .verifyComplete();
        }
    }

    @Test
    void buildVerifiablePresentationWithSelectedVCsTest() throws JsonProcessingException {
        String processId = "123";
        String authorizationToken = "authToken";
        VcSelectorResponse vcSelectorResponse = VcSelectorResponse.builder().build();
        String verifiablePresentation = "vp";

        when(presentationService.createSignedVerifiablePresentation(
                eq(processId),
                eq(authorizationToken),
                eq(vcSelectorResponse),
                anyString(),
                eq("vpWeb")
        )).thenReturn(Mono.just(verifiablePresentation));
        when(authorizationResponseService.buildAndPostAuthorizationResponseWithVerifiablePresentation(processId, vcSelectorResponse, verifiablePresentation, authorizationToken)).thenReturn(Mono.empty());

        StepVerifier.create(attestationExchangeServiceFacade.buildVerifiablePresentationWithSelectedVCs(processId, authorizationToken, vcSelectorResponse))
                .verifyComplete();
    }

}
