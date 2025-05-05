package es.in2.wallet.infrastructure.appconfiguration.adapter;

import es.in2.wallet.infrastructure.appconfiguration.service.GenericConfigAdapter;
import org.springframework.stereotype.Component;

@Component
public class YamlAppConfigurationAdapter implements GenericConfigAdapter {

    @Override
    public String getConfiguration(String key) {
        return key;
    }

}
