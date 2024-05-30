package es.in2.wallet.application.workflow.presentation.impl;

import es.in2.wallet.application.workflow.presentation.AttestationExchangeDOMEWorkflow;
import es.in2.wallet.domain.model.VcSelectorRequest;
import es.in2.wallet.domain.model.VcSelectorResponse;
import es.in2.wallet.domain.service.AuthorizationRequestService;
import es.in2.wallet.domain.service.AuthorizationResponseService;
import es.in2.wallet.domain.service.DomeVpTokenService;
import es.in2.wallet.domain.service.PresentationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttestationExchangeDOMEWorkflowImpl implements AttestationExchangeDOMEWorkflow {
    private final AuthorizationRequestService authorizationRequestService;
    private final DomeVpTokenService domeVpTokenService;
    private final PresentationService presentationService;
    private final AuthorizationResponseService authorizationResponseService;


    @Override
    public Mono<VcSelectorRequest> getSelectableCredentialsRequiredToBuildThePresentation(String processId, String authorizationToken, String qrContent) {
        log.info("ProcessID: {} - Processing a Verifiable Credential Login Request", processId);
        // Get Authorization Request executing the VC Login Request
        return  authorizationRequestService.getAuthorizationRequestFromAuthorizationRequestClaims(processId, qrContent)
                // Check which Verifiable Credentials are selectable
                .flatMap(authorizationRequest -> domeVpTokenService.getVpRequest(processId,authorizationToken,authorizationRequest));
    }
    @Override
    public Mono<Void> publishAuthorisationResponseWithSelectedVCs(String processId, String authorizationToken, VcSelectorResponse vcSelectorResponse) {
        // Get the Verifiable Credentials which will be used for the Presentation from the Wallet Data Service
        return presentationService.createEncodedVerifiablePresentationForDome(processId,authorizationToken,vcSelectorResponse)
                .flatMap(vpToken -> authorizationResponseService.sendDomeAuthorizationResponse(vpToken,vcSelectorResponse));
    }
}
