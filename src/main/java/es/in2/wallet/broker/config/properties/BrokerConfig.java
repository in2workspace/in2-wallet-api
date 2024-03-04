package es.in2.wallet.broker.config.properties;

import es.in2.wallet.configuration.service.GenericConfigAdapter;
import es.in2.wallet.configuration.util.ConfigAdapterFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BrokerConfig {
    private final GenericConfigAdapter genericConfigAdapter;
    private final BrokerProperties brokerProperties;

    public BrokerConfig(ConfigAdapterFactory configAdapterFactory, BrokerProperties brokerProperties) {
        this.genericConfigAdapter = configAdapterFactory.getAdapter();
        this.brokerProperties = brokerProperties;
    }

    public String getProvider() {
        return genericConfigAdapter.getConfiguration(brokerProperties.provider());
    }

    public String getExternalDomain() {
        return genericConfigAdapter.getConfiguration(brokerProperties.externalDomain());
    }

    public String getInternalDomain() {
        return genericConfigAdapter.getConfiguration(brokerProperties.internalDomain());
    }

    public String getPathEntities() {
        return genericConfigAdapter.getConfiguration(brokerProperties.paths().entities());
    }




}
