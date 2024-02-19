package es.in2.wallet.api.ebsi.comformance.service;

import es.in2.wallet.api.model.AuthorisationServerMetadata;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface IdTokenService {
    Mono<Map<String, String>> getIdTokenResponse(String processId, String did, AuthorisationServerMetadata authorisationServerMetadata, String jwt);
}
