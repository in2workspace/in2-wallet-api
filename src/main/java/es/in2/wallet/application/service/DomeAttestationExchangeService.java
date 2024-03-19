package es.in2.wallet.application.service;

import reactor.core.publisher.Mono;

import java.util.Map;

public interface DomeAttestationExchangeService {
    Mono<Void> getSelectableCredentialsRequiredToBuildThePresentation(String processId, String authorizationToken, String qrContent);
}
