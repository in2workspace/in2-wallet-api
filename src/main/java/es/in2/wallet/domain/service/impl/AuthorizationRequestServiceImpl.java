package es.in2.wallet.domain.service.impl;

import com.nimbusds.jose.JWSObject;
import es.in2.wallet.domain.model.AuthorizationRequest;
import es.in2.wallet.domain.service.AuthorizationRequestService;
import es.in2.wallet.infrastructure.core.config.WebClientConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static es.in2.wallet.domain.util.ApplicationConstants.BEARER;
import static es.in2.wallet.domain.util.ApplicationConstants.HEADER_AUTHORIZATION;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationRequestServiceImpl implements AuthorizationRequestService {
    private final WebClientConfig webClient;

    @Override
    public Mono<String> getAuthorizationRequestFromVcLoginRequest(String processId, String qrContent, String authorizationToken) {
        log.info("Processing a Verifiable Credential Login Request");
        // Get Authorization Request executing the VC Login Request
        return getJwtAuthorizationRequest(qrContent, authorizationToken)
                .doOnSuccess(response -> log.info("ProcessID: {} - Authorization Request Response: {}", processId, response))
                .onErrorResume(e -> {
                    log.error("ProcessID: {} - Error while processing Authorization Request from the Issuer: {}", processId, e.getMessage());
                    return Mono.error(new RuntimeException("Error while processing Authorization Request from the Issuer"));
                });
    }

    private Mono<String> getJwtAuthorizationRequest(String authorizationRequestUri, String authorizationToken) {
        return webClient.centralizedWebClient()
                .get()
                .uri(authorizationRequestUri)
                .header(HEADER_AUTHORIZATION, BEARER + authorizationToken)
                .exchangeToMono(response -> {
                    if (response.statusCode().is4xxClientError() || response.statusCode().is5xxServerError()) {
                        return Mono.error(new RuntimeException("There was an error retrieving authorisation request, error" + response));
                    }
                    else {
                        log.info("Authorization request in jwt format: {}", response);
                        return response.bodyToMono(String.class);
                    }
                });
    }

    @Override
    public Mono<AuthorizationRequest> getAuthorizationRequestFromJwtAuthorizationRequestClaim(String processId, String jwtAuthorizationRequestClaim) {
        try {
            JWSObject jwsObject = JWSObject.parse(jwtAuthorizationRequestClaim);
            String authorizationRequestClaim = jwsObject.getPayload().toJSONObject().get("auth_request").toString();
            return Mono.fromCallable(() -> AuthorizationRequest.fromString(authorizationRequestClaim));
        } catch (Exception e) {
            log.error("ProcessID: {} - Error while parsing Authorization Request: {}", processId, e.getMessage());
            return Mono.error(new RuntimeException("Error while parsing Authorization Request"));
        }
    }

    @Override
    public Mono<AuthorizationRequest> getAuthorizationRequestFromAuthorizationRequestClaims(String processId, String authorizationRequestClaims) {
        try {
            return Mono.fromCallable(() -> AuthorizationRequest.fromString(authorizationRequestClaims));
        } catch (Exception e) {
            log.error("ProcessID: {} - Error while parsing Authorization Request: {}", processId, e.getMessage());
            return Mono.error(new RuntimeException("Error while parsing Authorization Request"));
        }
    }

}
