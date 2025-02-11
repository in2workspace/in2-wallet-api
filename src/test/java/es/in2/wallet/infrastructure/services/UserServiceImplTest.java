package es.in2.wallet.infrastructure.services;

import es.in2.wallet.domain.entities.User;
import es.in2.wallet.domain.repositories.UserRepository;
import es.in2.wallet.domain.services.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userRepositoryService;

    @Test
    void testStoreUser_UserExists() {
        String processId = "process-123";
        String userIdStr = UUID.randomUUID().toString();
        UUID userUuid = UUID.fromString(userIdStr);

        User existingUser = User.builder().id(userUuid).build();
        when(userRepository.findByUserId(userUuid))
                .thenReturn(Mono.just(existingUser));

        Mono<UUID> result = userRepositoryService.storeUser(processId, userIdStr);

        StepVerifier.create(result)
                .expectNext(userUuid)
                .verifyComplete();

        verify(userRepository).findByUserId(userUuid);
        verify(userRepository, never()).save(any());
    }

    @Test
    void testStoreUser_UserDoesNotExist() {
        String processId = "process-456";
        String userIdStr = UUID.randomUUID().toString();
        UUID userUuid = UUID.fromString(userIdStr);

        when(userRepository.findByUserId(userUuid))
                .thenReturn(Mono.empty());

        // We only stub save(...) for this scenario
        User savedUser = User.builder().id(userUuid).build();
        when(userRepository.save(argThat(u -> u.getUserId().equals(userUuid))))
                .thenReturn(Mono.just(savedUser));

        Mono<UUID> result = userRepositoryService.storeUser(processId, userIdStr);

        StepVerifier.create(result)
                .expectNext(userUuid)
                .verifyComplete();

        verify(userRepository).findByUserId(userUuid);
        verify(userRepository).save(argThat(u -> u.getUserId().equals(userUuid)));
    }
}