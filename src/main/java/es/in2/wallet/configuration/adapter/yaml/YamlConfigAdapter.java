package es.in2.wallet.configuration.adapter.yaml;

import es.in2.wallet.configuration.model.ConfigProviderName;
import es.in2.wallet.configuration.service.GenericConfigAdapter;
import es.in2.wallet.configuration.util.ConfigSourceNameAnnotation;
import org.springframework.stereotype.Component;

@Component
@ConfigSourceNameAnnotation(name = ConfigProviderName.YAML)
public class YamlConfigAdapter implements GenericConfigAdapter {
    @Override
    public String getConfiguration(String key){
        return key;
    }
}
