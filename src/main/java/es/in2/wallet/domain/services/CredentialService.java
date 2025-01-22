package es.in2.wallet.domain.services;

import es.in2.wallet.application.dto.CredentialIssuerMetadata;
import es.in2.wallet.application.dto.CredentialResponseWithStatus;
import es.in2.wallet.application.dto.TokenResponse;
import reactor.core.publisher.Mono;

import java.util.List;

public interface CredentialService {
    Mono<CredentialResponseWithStatus> getCredential(String jwt, TokenResponse tokenResponse, CredentialIssuerMetadata credentialIssuerMetadata, String format, List<String> types);
    Mono<CredentialResponseWithStatus> getCredentialDomeDeferredCase(String transactionId, String accessToken, String deferredEndpoint);
}
