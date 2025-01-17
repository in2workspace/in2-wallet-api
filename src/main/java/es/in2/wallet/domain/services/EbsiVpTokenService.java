package es.in2.wallet.domain.services;

import es.in2.wallet.application.dto.AuthorisationServerMetadata;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface EbsiVpTokenService {
    Mono<Map<String, String>> getVpRequest(String processId, String authorizationToken, AuthorisationServerMetadata authorisationServerMetadata, String jwt);
}
