package es.in2.wallet.domain.services;

import es.in2.wallet.application.dto.CredentialIssuerMetadata;
import es.in2.wallet.application.dto.CredentialOffer;
import reactor.core.publisher.Mono;

public interface CredentialIssuerMetadataService {
    Mono<CredentialIssuerMetadata> getCredentialIssuerMetadataFromCredentialOffer(String processId, CredentialOffer credentialOffer);
}
