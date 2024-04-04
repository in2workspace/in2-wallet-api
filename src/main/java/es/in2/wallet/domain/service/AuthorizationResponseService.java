package es.in2.wallet.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import es.in2.wallet.domain.model.VcSelectorResponse;
import reactor.core.publisher.Mono;

public interface AuthorizationResponseService {
    Mono<String> buildAndPostAuthorizationResponseWithVerifiablePresentation(String processId, VcSelectorResponse vcSelectorResponse, String verifiablePresentation, String authorizationToken) throws JsonProcessingException;
    Mono<Void> sendDomeAuthorizationResponse(String vpToken, VcSelectorResponse vcSelectorResponse);
}
