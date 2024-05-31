package es.in2.wallet.infrastructure.broker.service.impl;

import es.in2.wallet.application.port.BrokerService;
import es.in2.wallet.infrastructure.broker.service.GenericBrokerService;
import es.in2.wallet.infrastructure.broker.util.BrokerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
public class BrokerServiceImpl implements BrokerService {

    private final GenericBrokerService brokerAdapter;

    public BrokerServiceImpl(BrokerFactory brokerFactory) {
        this.brokerAdapter = brokerFactory.getBrokerAdapter();
    }

    public Mono<Void> postEntity(String processId, String requestBody) {
        return brokerAdapter.postEntity(processId, requestBody);
    }

    public Mono<Optional<String>> getEntityById(String processId, String id) {
        return brokerAdapter.getEntityById(processId, id);
    }

    public Mono<Void> updateEntity(String processId, String id, String requestBody) {
        return brokerAdapter.updateEntityById(processId, id, requestBody);
    }
    public Mono<String> getAllCredentialsByUserId(String processId, String userId){
        return brokerAdapter.getAllCredentialsByUserId(processId,userId);
    }
    public Mono<String> getCredentialByIdAndUserId(String processId, String  userId, String credentialId){
        return brokerAdapter.getCredentialByIdAndUserId(processId,userId,credentialId);
    }
    public Mono<Void> deleteCredentialByIdAndUserId(String processId, String  userId, String credentialId){
        return brokerAdapter.deleteCredentialByIdAndUserId(processId,userId,credentialId);
    }
    public Mono<String> getCredentialByCredentialTypeAndUserId(String processId, String  userId, String credentialType){
        return brokerAdapter.getCredentialByCredentialTypeAndUserId(processId,userId,credentialType);
    }
    public Mono<String> getTransactionThatIsLinkedToACredential(String processId, String credentialId) {
        return brokerAdapter.getTransactionThatIsLinkedToACredential(processId,credentialId);
    }
    public Mono<Void> deleteTransactionByTransactionId(String processId, String transactionId){
        return brokerAdapter.deleteTransactionByTransactionId(processId,transactionId);
    }
    
}
