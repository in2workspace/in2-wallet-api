package es.in2.wallet.api.facade;

import es.in2.wallet.api.model.VcSelectorRequest;
import es.in2.wallet.api.model.VcSelectorResponse;
import reactor.core.publisher.Mono;

public interface AttestationExchangeServiceFacade {
    Mono<VcSelectorRequest> getSelectableCredentialsRequiredToBuildThePresentation(String processId, String authorizationToken, String qrContent);
    Mono<Void> buildVerifiablePresentationWithSelectedVCs(String processId, String authorizationToken, VcSelectorResponse vcSelectorResponse);
}
