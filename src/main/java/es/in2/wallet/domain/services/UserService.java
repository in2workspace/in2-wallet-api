package es.in2.wallet.domain.services;

import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserService {
    Mono<UUID> storeUser(String processId, String userId);
}
