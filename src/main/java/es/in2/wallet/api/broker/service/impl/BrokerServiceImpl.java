package es.in2.wallet.api.broker.service.impl;

import es.in2.wallet.api.broker.service.BrokerService;
import es.in2.wallet.api.broker.service.GenericBrokerService;
import es.in2.wallet.api.broker.util.BrokerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class BrokerServiceImpl implements BrokerService {

    private final GenericBrokerService brokerAdapter;

    public BrokerServiceImpl(BrokerFactory brokerFactory) {
        this.brokerAdapter = brokerFactory.getBrokerAdapter();
    }
    public Mono<Boolean> checkIfEntityAlreadyExist(String processId, String userId) {
        return brokerAdapter.checkIfEntityAlreadyExist(processId, userId);
    }

    public Mono<Void> postEntity(String processId, String authToken, String requestBody) {
        return brokerAdapter.postEntity(processId, authToken, requestBody);
    }

    public Mono<String> getEntityById(String processId, String userId) {
        return brokerAdapter.getEntityById(processId, userId);
    }

    public Mono<Void> updateEntity(String processId, String authToken, String requestBody) {
        return brokerAdapter.updateEntity(processId, authToken, requestBody);
    }


}
