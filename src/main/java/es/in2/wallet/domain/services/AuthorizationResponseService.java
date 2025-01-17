package es.in2.wallet.domain.services;

import es.in2.wallet.application.dto.VcSelectorResponse;
import reactor.core.publisher.Mono;

public interface AuthorizationResponseService {
    Mono<String> buildAndPostAuthorizationResponseWithVerifiablePresentation(String processId, VcSelectorResponse vcSelectorResponse, String verifiablePresentation, String authorizationToken);
}
