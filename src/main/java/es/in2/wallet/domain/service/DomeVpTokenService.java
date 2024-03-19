package es.in2.wallet.domain.service;

import es.in2.wallet.domain.model.AuthorizationRequest;
import reactor.core.publisher.Mono;

public interface DomeVpTokenService {
    Mono<Void> getVpRequest(String processId, String authorizationToken, AuthorizationRequest authorizationRequest);

}
