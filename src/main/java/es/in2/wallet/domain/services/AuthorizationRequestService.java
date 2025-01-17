package es.in2.wallet.domain.services;

import es.in2.wallet.application.dto.AuthorizationRequestOIDC4VP;
import reactor.core.publisher.Mono;

public interface AuthorizationRequestService {
    Mono<String> getJwtRequestObjectFromUri(java.lang.String processId, java.lang.String qrContent);

    Mono<AuthorizationRequestOIDC4VP> getAuthorizationRequestFromJwtAuthorizationRequestJWT(String processId, String jwtAuthorizationRequestClaim);
}
