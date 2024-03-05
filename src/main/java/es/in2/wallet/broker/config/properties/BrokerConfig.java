package es.in2.wallet.broker.config.properties;

import es.in2.wallet.configuration.service.GenericConfigAdapter;
import es.in2.wallet.configuration.util.ConfigAdapterFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BrokerConfig {
    private final GenericConfigAdapter genericConfigAdapter;
    private final BrokerProperties brokerProperties;

    // Variable for caching the configuration
    private String externalDomain;
    private String internalDomain;

    @PostConstruct
    public void init() {
        externalDomain = initExternalUrl();
        internalDomain = initInternalUrl();
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

    public String getInternalUrl() {
        return internalDomain;
    }

    private String initInternalUrl() {
        return String.format("%s://%s:%d",
                brokerProperties.internalUrl().scheme(),
                genericConfigAdapter.getConfiguration(brokerProperties.internalUrl().domain()),
                brokerProperties.internalUrl().port());
    }

    public String getPathEntities() {
        return brokerProperties.paths().entities();
    }




}
