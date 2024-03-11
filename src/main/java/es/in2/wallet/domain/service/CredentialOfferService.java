package es.in2.wallet.domain.service;

import es.in2.wallet.domain.model.CredentialOffer;
import reactor.core.publisher.Mono;

public interface CredentialOfferService {
    Mono<CredentialOffer> getCredentialOfferFromCredentialOfferUriWithAuthorizationToken(String processId, String credentialOfferUri, String authorizationToken);
    Mono<CredentialOffer> getCredentialOfferFromCredentialOfferUri(String processId, String credentialOfferUri);
}
