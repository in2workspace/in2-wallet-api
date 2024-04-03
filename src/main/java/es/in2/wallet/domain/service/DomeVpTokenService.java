package es.in2.wallet.domain.service;

import es.in2.wallet.domain.model.AuthorizationRequest;
import es.in2.wallet.domain.model.VcSelectorRequest;
import reactor.core.publisher.Mono;

public interface DomeVpTokenService {
    Mono<VcSelectorRequest> getVpRequest(String processId, String authorizationToken, AuthorizationRequest authorizationRequest);

}
