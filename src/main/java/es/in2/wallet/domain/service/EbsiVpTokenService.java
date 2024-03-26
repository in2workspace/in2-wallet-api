package es.in2.wallet.domain.service;

import es.in2.wallet.domain.model.AuthorisationServerMetadata;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface EbsiVpTokenService {
    Mono<Map<String, String>> getVpRequest(String processId, String authorizationToken, AuthorisationServerMetadata authorisationServerMetadata, String jwt);
}
