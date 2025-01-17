package es.in2.wallet.infrastructure.services;

import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserRepositoryService {
    Mono<UUID> storeUser(String processId, String userId);
}
