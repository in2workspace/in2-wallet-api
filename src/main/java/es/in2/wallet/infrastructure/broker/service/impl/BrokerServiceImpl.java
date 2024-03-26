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

    public Mono<Optional<String>>  getEntityById(String processId, String userId) {
        return brokerAdapter.getEntityById(processId, userId);
    }

    public Mono<Void> updateEntity(String processId, String userId, String requestBody) {
        return brokerAdapter.updateEntity(processId, userId, requestBody);
    }
    
}
