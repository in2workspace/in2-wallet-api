package es.in2.wallet.api.service;

import reactor.core.publisher.Mono;

import java.text.ParseException;

public interface CborGenerationService {
    Mono<String> generateCbor(String processId, String authorizationToken, String content) throws ParseException;
}
