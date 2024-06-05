package es.in2.wallet.infrastructure.broker.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import static es.in2.wallet.domain.util.ApplicationUtils.formatUrl;

@Configuration
public class BrokerConfig {

    private final BrokerProperties brokerProperties;

    private String internal;

    @PostConstruct
    public void init() {
        internal = initInternalUrl();
    }

    public BrokerConfig(BrokerProperties brokerProperties) {
        this.brokerProperties = brokerProperties;
    }

    public String getProvider() {
        return brokerProperties.provider();
    }

    public String getInternalUrl() {
        return internal;
    }

    private String initInternalUrl() {
        return formatUrl(brokerProperties.internalUrl().scheme(),
                brokerProperties.internalUrl().domain(),
                brokerProperties.internalUrl().port(),
                null);
    }

    public String getEntitiesPath() {
        return brokerProperties.paths().entities();
    }

}
