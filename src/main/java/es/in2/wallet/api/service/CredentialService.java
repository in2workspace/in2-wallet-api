package es.in2.wallet.api.service;

import es.in2.wallet.api.model.CredentialIssuerMetadata;
import es.in2.wallet.api.model.CredentialResponse;
import es.in2.wallet.api.model.TokenResponse;
import reactor.core.publisher.Mono;

import java.util.List;

public interface CredentialService {
    Mono<CredentialResponse> getCredential(String jwt, TokenResponse tokenResponse, CredentialIssuerMetadata credentialIssuerMetadata, String format, List<String> types);
}
