package es.in2.wallet.infrastructure.broker.config;

import es.in2.wallet.infrastructure.appconfiguration.service.GenericConfigAdapter;
import es.in2.wallet.infrastructure.appconfiguration.util.ConfigAdapterFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BrokerConfig {

    private final BrokerProperties brokerProperties;
    private final GenericConfigAdapter genericConfigAdapter;

    private String externalDomain;

    @PostConstruct
    public void init() {
        externalDomain = initExternalUrl();
    }

    public BrokerConfig(ConfigAdapterFactory configAdapterFactory, BrokerProperties brokerProperties) {
        this.genericConfigAdapter = configAdapterFactory.getAdapter();
        this.brokerProperties = brokerProperties;
    }

    public String getProvider() {
        return brokerProperties.provider();
    }

    public String getExternalUrl() {
        return externalDomain;
    }

    private String initExternalUrl() {
        return String.format("%s://%s:%d",
                brokerProperties.externalUrl().scheme(),
                genericConfigAdapter.getConfiguration(brokerProperties.externalUrl().domain()),
                brokerProperties.externalUrl().port());
    }

    public String getEntitiesPath() {
        return brokerProperties.paths().entities();
    }

}
