package es.in2.wallet.api.ebsi.comformance.service;

import es.in2.wallet.api.model.AuthorisationServerMetadata;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface VpTokenService {
    Mono<Map<String, String>> getVpRequest(String processId, String authorizationToken, AuthorisationServerMetadata authorisationServerMetadata, String jwt);
}
