package es.in2.wallet.domain.service.impl;

import es.in2.wallet.application.workflow.presentation.AttestationExchangeCommonWorkflow;
import es.in2.wallet.domain.model.AuthorizationRequest;
import es.in2.wallet.domain.model.VcSelectorRequest;
import es.in2.wallet.domain.service.DomeVpTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static es.in2.wallet.domain.util.ApplicationConstants.DEFAULT_SCOPE_FOR_DOME_VERIFIER;
import static es.in2.wallet.domain.util.ApplicationConstants.DEFAULT_VC_TYPES_FOR_DOME_VERIFIER;


@Slf4j
@Service
@RequiredArgsConstructor
public class DomeVpTokenServiceImpl implements DomeVpTokenService {
    private final AttestationExchangeCommonWorkflow attestationExchangeCommonWorkflow;

    /**
     * Initiates the process to exchange the authorization token and JWT for a VP Token Request,
     * logging the authorization response with the code upon success.
     *
     * @param processId An identifier for the process, used for logging.
     * @param authorizationToken The authorization token provided by the client.
     */
    @Override
    public Mono<VcSelectorRequest> getVpRequest(String processId, String authorizationToken, AuthorizationRequest authorizationRequest) {
        return completeVpTokenExchange(processId, authorizationToken, authorizationRequest)
                .doOnSuccess(tokenResponse -> log.info("ProcessID: {} - Token Response: {}", processId, tokenResponse));
    }


    /**
     * Completes the VP Token exchange process by building a VP token response and extracting query parameters from it.
     */
    private Mono<VcSelectorRequest> completeVpTokenExchange(String processId, String authorizationToken, AuthorizationRequest authorizationRequest) {
        return buildVpTokenResponse(processId,authorizationToken,authorizationRequest);
    }

    private Mono<VcSelectorRequest> buildVpTokenResponse(String processId, String authorizationToken, AuthorizationRequest authorizationRequest) {
        // Check if any of the scopes defined in DEFAULT_SCOPE_FOR_DOME_VERIFIER are present in the scope of AuthorizationRequest
        boolean scopeMatches = DEFAULT_SCOPE_FOR_DOME_VERIFIER.stream().anyMatch(scope -> authorizationRequest.scope().contains(scope));

        if (scopeMatches) {
            // If there is a match, use DEFAULT_VC_TYPES_FOR_DOME_VERIFIER as the list of VC types
            return attestationExchangeCommonWorkflow.getSelectableCredentialsRequiredToBuildThePresentation(processId, authorizationToken, DEFAULT_VC_TYPES_FOR_DOME_VERIFIER)
                    .flatMap(credentialsBasicInfos -> Mono.just(VcSelectorRequest.builder().selectableVcList(credentialsBasicInfos)
                            .redirectUri(authorizationRequest.redirectUri())
                            .state(authorizationRequest.state())
                            .build()));
        } else {
            // If there is no match, pass the scope from AuthorizationRequest directly
            return attestationExchangeCommonWorkflow.getSelectableCredentialsRequiredToBuildThePresentation(processId, authorizationToken, authorizationRequest.scope())
                    .flatMap(credentialsBasicInfos -> Mono.just(VcSelectorRequest.builder().selectableVcList(credentialsBasicInfos)
                            .redirectUri(authorizationRequest.redirectUri())
                            .state(authorizationRequest.state())
                            .build()));
        }
    }
}
