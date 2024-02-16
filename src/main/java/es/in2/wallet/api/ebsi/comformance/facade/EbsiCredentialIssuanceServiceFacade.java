package es.in2.wallet.api.ebsi.comformance.facade;

import reactor.core.publisher.Mono;

public interface EbsiCredentialIssuanceServiceFacade {
    Mono<Void> identifyAuthMethod(String processId, String authorizationToken, String qrContent);
}
