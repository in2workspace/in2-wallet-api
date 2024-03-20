package es.in2.wallet.application.service.impl;

import es.in2.wallet.application.service.DomeAttestationExchangeService;
import es.in2.wallet.domain.service.AuthorizationRequestService;
import es.in2.wallet.domain.service.DidKeyGeneratorService;
import es.in2.wallet.domain.service.DomeVpTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DomeAttestationExchangeServiceImpl implements DomeAttestationExchangeService {
    private final AuthorizationRequestService authorizationRequestService;
    private final DomeVpTokenService domeVpTokenService;
    private final DidKeyGeneratorService didKeyGeneratorService;


    @Override
    public Mono<Void> getSelectableCredentialsRequiredToBuildThePresentation(String processId, String authorizationToken, String qrContent) {
        log.info("ProcessID: {} - Processing a Verifiable Credential Login Request", processId);
        // Get Authorization Request executing the VC Login Request
        return  authorizationRequestService.getAuthorizationRequestFromAuthorizationRequestClaims(processId, qrContent)
                // Check which Verifiable Credentials are selectable
                .flatMap(authorizationRequest -> domeVpTokenService.getVpRequest(processId,authorizationToken,authorizationRequest));
    }
}
