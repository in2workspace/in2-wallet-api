package es.in2.wallet.domain.service;

import es.in2.wallet.domain.model.AuthorizationRequestOIDC4VP;
import reactor.core.publisher.Mono;

public interface AuthorizationRequestService {
    Mono<String> getJwtRequestObjectFromUri(java.lang.String processId, java.lang.String qrContent);

    Mono<AuthorizationRequestOIDC4VP> getAuthorizationRequestFromJwtAuthorizationRequestJWT(String processId, String jwtAuthorizationRequestClaim);
}
