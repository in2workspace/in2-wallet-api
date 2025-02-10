package es.in2.wallet.domain.services.impl;

import es.in2.wallet.domain.entities.User;
import es.in2.wallet.domain.repositories.UserRepository;
import es.in2.wallet.domain.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public Mono<UUID> storeUser(String processId, String userId) {
        // Convert the String userId to UUID
        UUID uuid = UUID.fromString(userId);
        Instant now = Instant.now();

        return userRepository.findById(uuid)
                // If user is found, skip saving and just return userId
                .flatMap(existingUser -> {
                    log.info("[{}] User {} already exists.", processId, userId);
                    // Return a non-empty Mono => switchIfEmpty won't run
                    return Mono.just(uuid);
                })
                // If user is not found, create the new user
                .switchIfEmpty(Mono.defer(() -> {
                    User newUser = User.builder()
                            .id(uuid)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();

                    return userRepository.save(newUser)
                            .doOnSuccess(u -> log.info("[{}] User {} created successfully.", processId, userId))
                            // We map the saved user to the same UUID
                            .map(u -> uuid);
                }));
    }
}



