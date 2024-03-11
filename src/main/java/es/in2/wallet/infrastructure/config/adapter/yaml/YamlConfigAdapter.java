package es.in2.wallet.infrastructure.config.adapter.yaml;

import es.in2.wallet.infrastructure.config.model.ConfigProviderName;
import es.in2.wallet.infrastructure.config.service.GenericConfigAdapter;
import es.in2.wallet.infrastructure.config.util.ConfigSourceNameAnnotation;
import org.springframework.stereotype.Component;

@Component
@ConfigSourceNameAnnotation(name = ConfigProviderName.YAML)
public class YamlConfigAdapter implements GenericConfigAdapter {
    @Override
    public String getConfiguration(String key){
        return key;
    }
}
