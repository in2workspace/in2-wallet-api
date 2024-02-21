package es.in2.wallet.api.ebsi.comformance.facade;

import reactor.core.publisher.Mono;

public interface EbsiCredentialServiceFacade {
    Mono<Void> identifyAuthMethod(String processId, String authorizationToken, String qrContent);
}
