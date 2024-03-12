package es.in2.wallet.infrastructure.appconfiguration.adapter;

import es.in2.wallet.infrastructure.appconfiguration.model.ConfigProviderName;
import es.in2.wallet.infrastructure.appconfiguration.service.GenericConfigAdapter;
import es.in2.wallet.infrastructure.appconfiguration.util.ConfigSourceNameAnnotation;
import org.springframework.stereotype.Component;

@Component
@ConfigSourceNameAnnotation(name = ConfigProviderName.YAML)
public class YamlAppConfigurationAdapter implements GenericConfigAdapter {

    @Override
    public String getConfiguration(String key) {
        return key;
    }

}
