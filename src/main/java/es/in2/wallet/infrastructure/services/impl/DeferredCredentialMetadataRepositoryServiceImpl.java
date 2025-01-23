package es.in2.wallet.infrastructure.services.impl;

import es.in2.wallet.domain.entities.DeferredCredentialMetadata;
import es.in2.wallet.domain.exceptions.NoSuchDeferredCredentialMetadataException;
import es.in2.wallet.infrastructure.repositories.DeferredCredentialMetadataRepository;
import es.in2.wallet.infrastructure.services.DeferredCredentialMetadataRepositoryService;
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
public class DeferredCredentialMetadataRepositoryServiceImpl implements DeferredCredentialMetadataRepositoryService {

    private final DeferredCredentialMetadataRepository deferredCredentialMetadataRepository;

    @Override
    public Mono<UUID> saveDeferredCredentialMetadata(
            String processId,
            UUID credentialId,
            String transactionId,
            String accessToken,
            String deferredEndpoint
    ) {
        Timestamp currentTimestamp = new Timestamp(Instant.now().toEpochMilli());
        return deferredCredentialMetadataRepository.save(
                DeferredCredentialMetadata.builder()
                        .credentialId(credentialId)
                        .transactionId(UUID.fromString(transactionId))
                        .accessToken(accessToken)
                        .deferredEndpoint(deferredEndpoint)
                        .createdAt(currentTimestamp)
                        .updatedAt(currentTimestamp)
                .build())
                .then(Mono.just(credentialId))
                .doOnSuccess(credentialUuid -> log.info("[Process ID: {}] Deferred credential metadata for credential ID {} saved successfully.", processId, credentialUuid.toString()));
    }

    @Override
    public Mono<DeferredCredentialMetadata> getDeferredCredentialMetadataByCredentialId(
            String processId,
            String credentialId
    ) {
        return findOrError(credentialId)
                .doOnSuccess(metadata ->
                        log.info("[Process ID: {}] Found deferred credential metadata for credential ID {}.",
                                processId, credentialId)
                )
                .doOnError(error ->
                        log.error("[Process ID: {}] Error retrieving deferred credential metadata for credentialId {} - {}",
                                processId, credentialId, error.getMessage(), error)
                );
    }

    @Override
    public Mono<Void> deleteDeferredCredentialMetadataByCredentialId(
            String processId,
            String credentialId
    ) {
        return findOrError(credentialId)
                .flatMap(deferredCredentialMetadataRepository::delete)
                .then()
                .doOnSuccess(unused ->
                        log.info("[Process ID: {}] Deleted deferred credential metadata for credential ID {}.",
                                processId, credentialId)
                )
                .doOnError(error ->
                        log.error("[Process ID: {}] Error deleting deferred credential metadata for credentialId {} - {}",
                                processId, credentialId, error.getMessage(), error)
                );
    }

    @Override
    public Mono<Void> updateDeferredCredentialMetadataTransactionIdByCredentialId(
            String processId,
            String credentialId,
            String transactionId
    ) {
        return findOrError(credentialId)
                .flatMap(metadata -> {
                    metadata.setTransactionId(UUID.fromString(transactionId));
                    metadata.setUpdatedAt(new Timestamp(Instant.now().toEpochMilli()));
                    return deferredCredentialMetadataRepository.save(metadata);
                })
                .then()
                .doOnSuccess(unused ->
                        log.info("[Process ID: {}] Updated deferred credential metadata for credential ID {}.",
                                processId, credentialId)
                )
                .doOnError(error ->
                        log.error("[Process ID: {}] Error updating deferred credential metadata for credentialId {} - {}",
                                processId, credentialId, error.getMessage(), error)
                );
    }

    private Mono<DeferredCredentialMetadata> findOrError(String credentialId) {
        return deferredCredentialMetadataRepository.findByCredentialId(UUID.fromString(credentialId))
                .switchIfEmpty(Mono.error(new NoSuchDeferredCredentialMetadataException(
                        "No deferred credential metadata found for credentialId: " + credentialId
                )));
    }

}
