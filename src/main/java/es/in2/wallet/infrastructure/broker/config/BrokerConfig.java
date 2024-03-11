package es.in2.wallet.infrastructure.broker.config;

import es.in2.wallet.infrastructure.broker.config.properties.BrokerProperties;
import es.in2.wallet.infrastructure.config.service.GenericConfigAdapter;
import es.in2.wallet.infrastructure.config.util.ConfigAdapterFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BrokerConfig {

    private final BrokerProperties brokerProperties;

    // todo: No creo que debiera implementar el GenericConfigAdapter sino el ConfigAdapter, ¿es así?
    private final GenericConfigAdapter genericConfigAdapter;

    // todo: ¿Por qué se usa este abordaje de variables y PostConstruct?
    // Variable for caching the configuration
    private String externalDomain;
    private String internalDomain;

    @PostConstruct
    public void init() {
        externalDomain = initExternalUrl();
        internalDomain = initInternalUrl();
    }

    public BrokerConfig(ConfigAdapterFactory configAdapterFactory, BrokerProperties brokerProperties) {
        // todo: entiendo que no haría falta si se implementa el ConfigAdapter en lugar del GenericConfigAdapter
        this.genericConfigAdapter = configAdapterFactory.getAdapter();
        this.brokerProperties = brokerProperties;
    }

    public String getProvider() {
        return brokerProperties.provider();
    }

    public String getExternalUrl() {
        return externalDomain;
    }

    // todo: ¿Qué hacen los inits?
    private String initExternalUrl() {
        return String.format("%s://%s:%d",
                brokerProperties.externalUrl().scheme(),
                genericConfigAdapter.getConfiguration(brokerProperties.externalUrl().domain()),
                brokerProperties.externalUrl().port());
    }

    // fixme: si no se usa, se debe borrar
    public String getInternalUrl() {
        return internalDomain;
    }

    private String initInternalUrl() {
        return String.format("%s://%s:%d",
                brokerProperties.internalUrl().scheme(),
                genericConfigAdapter.getConfiguration(brokerProperties.internalUrl().domain()),
                brokerProperties.internalUrl().port());
    }

    // fixme: cleanCode, aunque la descripción del atributo sea como los paquetes, properties.path.entities()
    //  la variable debería leerse como getEntitiesPath(), es decir, dame el path de las entidades.
    //  getPathEntities() = obtener entidades de ruta, getEntitiesPath() = obtener ruta de entidades.
    public String getPathEntities() {
        return brokerProperties.paths().entities();
    }

}
