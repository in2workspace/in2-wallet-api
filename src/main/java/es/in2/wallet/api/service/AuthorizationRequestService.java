package es.in2.wallet.api.service;

import es.in2.wallet.api.model.AuthorizationRequest;
import reactor.core.publisher.Mono;

public interface AuthorizationRequestService {
    Mono<String> getAuthorizationRequestFromVcLoginRequest(String processId, String qrContent);

    Mono<AuthorizationRequest> getAuthorizationRequestFromJwtAuthorizationRequestClaim(String processId, String jwtAuthorizationRequestClaim);
}
