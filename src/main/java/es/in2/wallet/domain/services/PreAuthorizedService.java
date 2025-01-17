package es.in2.wallet.domain.services;

import es.in2.wallet.application.dto.AuthorisationServerMetadata;
import es.in2.wallet.application.dto.CredentialOffer;
import es.in2.wallet.application.dto.TokenResponse;
import reactor.core.publisher.Mono;

public interface PreAuthorizedService {
    Mono<TokenResponse> getPreAuthorizedToken(String processId, CredentialOffer credentialOffer, AuthorisationServerMetadata authorisationServerMetadata, String authorizationToken);
}
