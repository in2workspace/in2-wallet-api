package es.in2.wallet.api.ebsi.comformance.service;

import es.in2.wallet.api.model.CredentialIssuerMetadata;
import es.in2.wallet.api.model.CredentialResponse;
import es.in2.wallet.api.model.TokenResponse;
import reactor.core.publisher.Mono;

import java.util.List;

public interface CredentialEbsiService {
    Mono<CredentialResponse> getCredential(String processId, String did, TokenResponse tokenResponse, CredentialIssuerMetadata credentialIssuerMetadata, String format, List<String> types);
}