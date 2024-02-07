package es.in2.wallet.api.service;

import reactor.core.publisher.Mono;

import java.util.List;

public interface PresentationService {
    Mono<String> createUnsignedVerifiablePresentation(String processId, List<String> verifiableCredentialsList);
}
