package es.in2.wallet.domain.service;

import reactor.core.publisher.Mono;

import java.text.ParseException;

public interface CborGenerationService {
    Mono<String> generateCbor(String processId, String content) throws ParseException;
}
