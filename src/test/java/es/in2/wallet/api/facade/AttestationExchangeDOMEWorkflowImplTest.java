package es.in2.wallet.api.facade;

import es.in2.wallet.application.workflow.presentation.impl.AttestationExchangeDOMEWorkflowImpl;
import es.in2.wallet.domain.model.AuthorizationRequest;
import es.in2.wallet.domain.model.VcSelectorRequest;
import es.in2.wallet.domain.model.VcSelectorResponse;
import es.in2.wallet.domain.service.AuthorizationRequestService;
import es.in2.wallet.domain.service.AuthorizationResponseService;
import es.in2.wallet.domain.service.DomeVpTokenService;
import es.in2.wallet.domain.service.PresentationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttestationExchangeDOMEWorkflowImplTest {

    @Mock
    private AuthorizationRequestService authorizationRequestService;
    @Mock
    private DomeVpTokenService domeVpTokenService;
    @Mock
    private PresentationService presentationService;
    @Mock
    private AuthorizationResponseService authorizationResponseService;

    @InjectMocks
    private AttestationExchangeDOMEWorkflowImpl domeAttestationExchangeService;
    @Test
    void getSelectableCredentialsRequiredToBuildThePresentationTest() {
        String processId = "processId";
        String authorizationToken = "authToken";
        String qrContent = "qrContent";
        AuthorizationRequest authorizationRequest = mock(AuthorizationRequest.class); // Asumiendo que tienes una clase llamada AuthorizationRequest
        VcSelectorRequest vcSelectorRequest = mock(VcSelectorRequest.class); // Asumiendo que tienes una clase llamada VcSelectorRequest

        when(authorizationRequestService.getAuthorizationRequestFromAuthorizationRequestClaims(processId, qrContent))
                .thenReturn(Mono.just(authorizationRequest));
        when(domeVpTokenService.getVpRequest(processId, authorizationToken, authorizationRequest))
                .thenReturn(Mono.just(vcSelectorRequest));

        StepVerifier.create(domeAttestationExchangeService.getSelectableCredentialsRequiredToBuildThePresentation(processId, authorizationToken, qrContent))
                .expectNext(vcSelectorRequest)
                .verifyComplete();

        verify(authorizationRequestService).getAuthorizationRequestFromAuthorizationRequestClaims(processId, qrContent);
        verify(domeVpTokenService).getVpRequest(processId, authorizationToken, authorizationRequest);
    }
    @Test
    void buildAndSendVerifiablePresentationWithSelectedVCsForDomeTest() {
        String processId = "processId";
        String authorizationToken = "authToken";
        VcSelectorResponse vcSelectorResponse = mock(VcSelectorResponse.class);
        String vpToken = "vpToken";

        when(presentationService.createEncodedVerifiablePresentationForDome(processId, authorizationToken, vcSelectorResponse))
                .thenReturn(Mono.just(vpToken));
        when(authorizationResponseService.sendDomeAuthorizationResponse(vpToken, vcSelectorResponse))
                .thenReturn(Mono.empty());

        StepVerifier.create(domeAttestationExchangeService.publishAuthorisationResponseWithSelectedVCs(processId, authorizationToken, vcSelectorResponse))
                .verifyComplete();

        verify(presentationService).createEncodedVerifiablePresentationForDome(processId, authorizationToken, vcSelectorResponse);
        verify(authorizationResponseService).sendDomeAuthorizationResponse(vpToken, vcSelectorResponse);
    }


}
