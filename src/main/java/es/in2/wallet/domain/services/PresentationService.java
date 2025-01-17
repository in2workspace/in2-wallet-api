package es.in2.wallet.domain.services;

import es.in2.wallet.application.dto.CredentialsBasicInfo;
import es.in2.wallet.application.dto.VcSelectorResponse;
import reactor.core.publisher.Mono;

public interface PresentationService {
    Mono<String> createSignedVerifiablePresentation(String processId, String authorizationToken, VcSelectorResponse vcSelectorResponse,String nonce, String audience);
    Mono<String> createSignedVerifiablePresentation(String processId, String authorizationToken, CredentialsBasicInfo credentialsBasicInfo, String nonce, String audience);
    Mono<String> createEncodedVerifiablePresentationForDome(String processId, String authorizationToken, VcSelectorResponse vcSelectorResponse);
}
