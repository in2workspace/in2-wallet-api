package es.in2.wallet.application.port;

import reactor.core.publisher.Mono;

import java.util.Optional;

public interface BrokerService {

    Mono<Void> postEntity(String processId, String requestBody);

    Mono<Optional<String>> getEntityById(String processId, String id);
    Mono<String> getAllCredentialsByUserId(String processId, String userId);
    Mono<String> getCredentialByIdAndUserId(String processId, String credentialId, String  userId);
    Mono<Void> deleteCredentialByIdAndUserId(String processId, String credentialId, String  userId);
    Mono<String> getCredentialByCredentialTypeAndUserId(String processId, String credentialType, String  userId);
    Mono<String> getTransactionThatIsLinkedToACredential(String processId, String credentialId);

    Mono<Void> updateEntity(String processId, String id, String requestBody);
    Mono<Void> deleteTransactionByTransactionId(String processId, String transactionId);

}
