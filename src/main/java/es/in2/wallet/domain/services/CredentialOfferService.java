package es.in2.wallet.domain.services;

import es.in2.wallet.application.dto.CredentialOffer;
import reactor.core.publisher.Mono;

public interface CredentialOfferService {
    Mono<CredentialOffer> getCredentialOfferFromCredentialOfferUri(String processId, String credentialOfferUri);
}
