package es.in2.wallet.api.service;

import es.in2.wallet.api.model.VcSelectorResponse;
import reactor.core.publisher.Mono;

public interface AuthorizationResponseService {
    Mono<String> buildAndPostAuthorizationResponseWithVerifiablePresentation(String processId, VcSelectorResponse vcSelectorResponse, String verifiablePresentation);
    Mono<String> getDescriptorMapping(String processId, String verifiablePresentation);
}
