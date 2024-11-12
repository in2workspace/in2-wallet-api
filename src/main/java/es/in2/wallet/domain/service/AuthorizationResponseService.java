package es.in2.wallet.domain.service;

import es.in2.wallet.domain.model.VcSelectorResponse;
import reactor.core.publisher.Mono;

public interface AuthorizationResponseService {
    Mono<String> buildAndPostAuthorizationResponseWithVerifiablePresentation(String processId, VcSelectorResponse vcSelectorResponse, String verifiablePresentation, String authorizationToken);
}
