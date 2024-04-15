package es.in2.wallet.application.service;

import es.in2.wallet.domain.model.VcSelectorRequest;
import es.in2.wallet.domain.model.VcSelectorResponse;
import reactor.core.publisher.Mono;


public interface DomeAttestationExchangeService {
    Mono<VcSelectorRequest> getSelectableCredentialsRequiredToBuildThePresentation(String processId, String authorizationToken, String qrContent);
    public Mono<Void> buildAndSendVerifiablePresentationWithSelectedVCsForDome(String processId, String authorizationToken, VcSelectorResponse vcSelectorResponse);
}
