package es.in2.wallet.api.ebsi.comformance.service;

import es.in2.wallet.api.model.AuthorisationServerMetadata;
import es.in2.wallet.api.model.TokenResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface AuthorisationResponseService {
    Mono<TokenResponse> sendTokenRequest(String codeVerifier, String did, AuthorisationServerMetadata authorisationServerMetadata, Map<String, String> params);
}
