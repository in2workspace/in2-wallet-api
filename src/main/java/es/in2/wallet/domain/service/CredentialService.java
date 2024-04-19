package es.in2.wallet.domain.service;

import es.in2.wallet.domain.model.CredentialIssuerMetadata;
import es.in2.wallet.domain.model.CredentialResponse;
import es.in2.wallet.domain.model.TokenResponse;
import reactor.core.publisher.Mono;

import java.util.List;

public interface CredentialService {
    Mono<CredentialResponse> getCredential(String jwt, TokenResponse tokenResponse, CredentialIssuerMetadata credentialIssuerMetadata, String format, List<String> types);
    Mono<CredentialResponse> getCredentialDomeDeferredCase(String transactionId, String accessToken, String deferredEndpoint);
}
