package es.in2.wallet.infrastructure.services.impl;

import es.in2.wallet.domain.entities.User;
import es.in2.wallet.infrastructure.repositories.UserRepository;
import es.in2.wallet.infrastructure.services.UserRepositoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRepositoryServiceImpl implements UserRepositoryService {
    private final UserRepository userRepository;

    @Override
    public Mono<UUID> storeUser(String processId, String userId) {
        UUID uuid = UUID.fromString(userId);
        Timestamp currentTimestamp = new Timestamp(Instant.now().toEpochMilli());

        log.debug("[Process ID: {}] Start processing user with ID {}", processId, userId);

        return userRepository.findById(uuid)
                .flatMap(existingUser -> {
                    log.info("[Process ID: {}] User with ID {} already exists.", processId, userId);
                    return Mono.empty();
                })
                .switchIfEmpty(
                        userRepository.save(User.builder()
                                        .userId(uuid)
                                        .createdAt(currentTimestamp)
                                        .updatedAt(currentTimestamp)
                                        .build())
                                .doOnSuccess(user -> log.info("[Process ID: {}] User with ID {} created successfully.", processId, userId))
                                .then()
                )
                .doOnTerminate(() -> log.debug("[Process ID: {}] Finished processing user with ID {}", processId, userId))
                .then(Mono.just(uuid));
    }
}



