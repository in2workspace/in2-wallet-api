package es.in2.wallet.api.service;

import es.in2.wallet.api.model.VcSelectorResponse;
import reactor.core.publisher.Mono;

public interface PresentationService {
    Mono<String> createSignedVerifiablePresentation(String processId, String authorizationToken, VcSelectorResponse vcSelectorResponse);
}
