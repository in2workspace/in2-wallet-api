package es.in2.wallet.domain.service;

import es.in2.wallet.domain.model.AuthorisationServerMetadata;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface EbsiIdTokenService {
    Mono<Map<String, String>> getIdTokenResponse(String processId, String did, AuthorisationServerMetadata authorisationServerMetadata, String jwt);
}
