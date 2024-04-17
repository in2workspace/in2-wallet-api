package es.in2.wallet.application.port;

import reactor.core.publisher.Mono;

import java.util.Optional;

public interface BrokerService {

    Mono<Void> postEntity(String processId, String requestBody);

    Mono<Optional<String>> getUserEntityById(String processId, String userId);
    Mono<String> getCredentialsThatBelongToUser(String processId, String userId);
    Mono<String> getCredentialByIdThatBelongToUser(String processId, String  userId, String credentialId);
    Mono<Void> deleteCredentialByIdThatBelongToUser(String processId, String  userId, String credentialId);
    Mono<String> getCredentialByCredentialTypeThatBelongToUser(String processId, String  userId, String credentialType);
    Mono<String> getTransactionThatIsLinkedToACredential(String processId, String credentialId);

    Mono<Void> updateEntity(String processId,  String userId, String requestBody);

}
