package es.in2.wallet.domain.services;

import es.in2.wallet.application.dto.AuthorisationServerMetadata;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface EbsiIdTokenService {
    Mono<Map<String, String>> getIdTokenResponse(String processId, String did, AuthorisationServerMetadata authorisationServerMetadata, String jwt);
}
