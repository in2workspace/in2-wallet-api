package es.in2.wallet.application.workflow.presentation.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import es.in2.wallet.application.port.BrokerService;
import es.in2.wallet.application.workflow.presentation.AttestationExchangeCommonWorkflow;
import es.in2.wallet.domain.exception.FailedDeserializingException;
import es.in2.wallet.domain.model.AuthorizationRequestOIDC4VP;
import es.in2.wallet.domain.model.CredentialsBasicInfo;
import es.in2.wallet.domain.model.VcSelectorRequest;
import es.in2.wallet.domain.model.VcSelectorResponse;
import es.in2.wallet.domain.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static es.in2.wallet.domain.util.ApplicationConstants.LEAR_CREDENTIAL_EMPLOYEE_SCOPE;
import static es.in2.wallet.domain.util.ApplicationUtils.getUserIdFromToken;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttestationExchangeCommonWorkflowImpl implements AttestationExchangeCommonWorkflow {

    private final AuthorizationRequestService authorizationRequestService;
    private final AuthorizationResponseService authorizationResponseService;
    private final VerifierValidationService verifierValidationService;
    private final DataService dataService;
    private final BrokerService brokerService;
    private final PresentationService presentationService;

    @Override
    public Mono<VcSelectorRequest> processAuthorizationRequest(String processId, String authorizationToken, String qrContent) {
        log.info("ProcessID: {} - Processing a Verifiable Credential Login Request", processId);
        return authorizationRequestService.getJwtRequestObjectFromUri(processId, qrContent)
                .flatMap(jwtAuthorizationRequest -> verifierValidationService.verifyIssuerOfTheAuthorizationRequest(processId, jwtAuthorizationRequest))
                .flatMap(jwtAuthorizationRequest -> authorizationRequestService.getAuthorizationRequestFromJwtAuthorizationRequestJWT(processId, jwtAuthorizationRequest))
                .flatMap(authorizationRequest -> getSelectableCredentialsRequiredToBuildThePresentation(processId, authorizationToken, authorizationRequest.scope())
                .flatMap(credentials -> buildSelectableVCsRequest(authorizationRequest,credentials)));
    }


    @Override
    public Mono<List<CredentialsBasicInfo>> getSelectableCredentialsRequiredToBuildThePresentation(String processId, String authorizationToken, List<String> scope) {
        return getUserIdFromToken(authorizationToken)
                .flatMap(userId -> Flux.fromIterable(scope)
                        .flatMap(element -> {
                            // Verificar si el elemento es igual a LEAR_CREDENTIAL_EMPLOYEE_SCOPE
                            String credentialType = element.equals(LEAR_CREDENTIAL_EMPLOYEE_SCOPE)
                                    ? "LEARCredentialEmployee" // Cambia a lo que sea necesario
                                    : element;

                            // Llamar al brokerService con el tipo de credencial adecuado
                            return brokerService.getCredentialByCredentialTypeAndUserId(processId, credentialType, userId)
                                    .flatMap(dataService::getUserVCsInJson);
                        })
                        .collectList()  // This will collect all lists into a single list
                        .flatMap(lists -> {
                            List<CredentialsBasicInfo> allCredentials = new ArrayList<>();
                            lists.forEach(allCredentials::addAll); // Combine all lists into one
                            return Mono.just(allCredentials);
                        })
                );
    }

    private Mono<VcSelectorRequest> buildSelectableVCsRequest(AuthorizationRequestOIDC4VP authorizationRequestOIDC4VP, List<CredentialsBasicInfo> selectableVCs) {
        return Mono.fromCallable(() -> VcSelectorRequest.builder()
                .redirectUri(authorizationRequestOIDC4VP.responseUri())
                .state(authorizationRequestOIDC4VP.state())
                .nonce(authorizationRequestOIDC4VP.nonce())
                .selectableVcList(selectableVCs)
                .build());
    }

    @Override
    public Mono<Void> buildVerifiablePresentationWithSelectedVCs(String processId, String authorizationToken, VcSelectorResponse vcSelectorResponse) {
        // Get the Verifiable Credentials which will be used for the Presentation from the Wallet Data Service
        return
                // Create the Verifiable Presentation
               generateAudience()
                       .flatMap(audience -> presentationService.createSignedVerifiablePresentation(processId, authorizationToken, vcSelectorResponse, vcSelectorResponse.nonce(), audience)
                       )

                // Build the Authentication Response
                // Send the Authentication Response to the Verifier
                .flatMap(verifiablePresentation ->
                {
                    try {
                        return authorizationResponseService.buildAndPostAuthorizationResponseWithVerifiablePresentation(processId, vcSelectorResponse, verifiablePresentation, authorizationToken);
                    } catch (JsonProcessingException e) {
                        return Mono.error(new FailedDeserializingException("Error while deserializing Credential: " + e));
                    }
                })
                .then();
    }


    private static Mono<String> generateAudience() {
        return Mono.just("https://self-issued.me/v2");
    }

}
