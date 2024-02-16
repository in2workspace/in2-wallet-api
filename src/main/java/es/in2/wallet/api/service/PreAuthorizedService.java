package es.in2.wallet.api.service;

import es.in2.wallet.api.model.AuthorisationServerMetadata;
import es.in2.wallet.api.model.CredentialOffer;
import es.in2.wallet.api.model.TokenResponse;
import reactor.core.publisher.Mono;

public interface PreAuthorizedService {
    Mono<TokenResponse> getPreAuthorizedToken(String processId, CredentialOffer credentialOffer, AuthorisationServerMetadata authorisationServerMetadata, String authorizationToken);
}
