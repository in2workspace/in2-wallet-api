package es.in2.wallet.broker.service;

import reactor.core.publisher.Mono;


public interface GenericBrokerService {

    Mono<Boolean> checkIfEntityAlreadyExist(String processId, String userId);
    Mono<Void> postEntity(String processId, String  userId, String requestBody);

    Mono<String> getEntityById(String processId,  String  userId);

    Mono<Void> updateEntity(String processId,  String  userId, String requestBody);

}
