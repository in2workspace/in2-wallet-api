package es.in2.wallet.infrastructure.repositories;

import es.in2.wallet.domain.entities.Credential;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Repository
public interface CredentialRepository extends ReactiveCrudRepository<Credential, UUID> {
    Mono<List<Credential>> findCredentialsByUserId(UUID userId);
}
