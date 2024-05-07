package es.in2.wallet.infrastructure.broker.service;

import reactor.core.publisher.Mono;

import java.util.Optional;


public interface GenericBrokerService {

    Mono<Void> postEntity(String processId, String requestBody);

    Mono<Optional<String>> getEntityById(String processId, String entityId);
    Mono<String> getAllCredentialsByUserId(String processId, String userId);
    Mono<String> getCredentialByIdAndUserId(String processId, String  userId, String credentialId);
    Mono<Void> deleteCredentialByIdAndUserId(String processId, String  userId, String credentialId);
    Mono<String> getCredentialByCredentialTypeAndUserId(String processId, String  userId, String credentialType);
    Mono<String> getTransactionThatIsLinkedToACredential(String processId, String credentialId);
    Mono<Void> updateEntity(String processId,  String userId, String requestBody);
    Mono<Void> deleteTransactionByTransactionId(String processId, String transactionId);

}
