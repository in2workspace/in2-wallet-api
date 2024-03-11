package es.in2.wallet.api.service;

import es.in2.wallet.api.model.CredentialOffer;
import reactor.core.publisher.Mono;

public interface CredentialOfferService {
    Mono<CredentialOffer> getCredentialOfferFromCredentialOfferUriWithAuthorizationToken(String processId, String credentialOfferUri, String authorizationToken);
    Mono<CredentialOffer> getCredentialOfferFromCredentialOfferUri(String processId, String credentialOfferUri);
}
