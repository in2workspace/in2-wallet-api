package es.in2.wallet.infrastructure.broker.util;

import es.in2.wallet.infrastructure.broker.adapter.OrionLdAdapter;
import es.in2.wallet.infrastructure.broker.adapter.ScorpioAdapter;
import es.in2.wallet.infrastructure.broker.config.BrokerConfig;
import es.in2.wallet.infrastructure.broker.service.GenericBrokerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BrokerFactory {

    private final BrokerConfig brokerConfig;
    private final ScorpioAdapter scorpioAdapter;
    private final OrionLdAdapter orionLdAdapter;

    public GenericBrokerService getBrokerAdapter() {
        return switch (brokerConfig.getProvider()) {
            case "scorpio" -> scorpioAdapter;
            case "orion-ld" -> orionLdAdapter;
            default -> throw new IllegalArgumentException("Invalid IAM provider: " + brokerConfig.getProvider());
        };
    }

}
