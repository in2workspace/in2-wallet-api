package es.in2.wallet.domain.service;

import es.in2.wallet.domain.model.AuthorisationServerMetadata;
import es.in2.wallet.domain.model.TokenResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface EbsiAuthorisationResponseService {
    Mono<TokenResponse> sendTokenRequest(String codeVerifier, String did, AuthorisationServerMetadata authorisationServerMetadata, Map<String, String> params);
}
