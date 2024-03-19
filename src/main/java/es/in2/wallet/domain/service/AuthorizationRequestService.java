package es.in2.wallet.domain.service;

import es.in2.wallet.domain.model.AuthorizationRequest;
import reactor.core.publisher.Mono;

public interface AuthorizationRequestService {
    Mono<String> getAuthorizationRequestFromVcLoginRequest(String processId, String qrContent, String authorizationToken);

    Mono<AuthorizationRequest> getAuthorizationRequestFromJwtAuthorizationRequestClaim(String processId, String jwtAuthorizationRequestClaim);
    Mono<AuthorizationRequest> getAuthorizationRequestFromAuthorizationRequestClaims(String processId, String authorizationRequestClaims);
}
