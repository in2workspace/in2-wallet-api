package es.in2.wallet.api.service;

import es.in2.wallet.api.model.AuthorisationServerMetadata;
import es.in2.wallet.api.model.CredentialIssuerMetadata;
import reactor.core.publisher.Mono;

public interface AuthorisationServerMetadataService {
    Mono<AuthorisationServerMetadata> getAuthorizationServerMetadataFromCredentialIssuerMetadata(String processId, CredentialIssuerMetadata credentialIssuerMetadata);
}
