package es.in2.wallet.api.ebsi.comformance.service;

import es.in2.wallet.api.model.AuthorisationServerMetadata;
import es.in2.wallet.api.model.TokenResponse;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

public interface VpTokenService {
    Mono<TokenResponse> getVpRequest(String processId, String authorizationToken, AuthorisationServerMetadata authorisationServerMetadata, String did, Tuple2<String, String> jwtAndCodeVerifier);
}
