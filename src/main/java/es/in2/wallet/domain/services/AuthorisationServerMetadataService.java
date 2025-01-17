package es.in2.wallet.domain.services;

import es.in2.wallet.application.dto.AuthorisationServerMetadata;
import es.in2.wallet.application.dto.CredentialIssuerMetadata;
import reactor.core.publisher.Mono;

public interface AuthorisationServerMetadataService {
    Mono<AuthorisationServerMetadata> getAuthorizationServerMetadataFromCredentialIssuerMetadata(String processId, CredentialIssuerMetadata credentialIssuerMetadata);
}
