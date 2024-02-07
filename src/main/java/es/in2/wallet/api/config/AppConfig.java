package es.in2.wallet.api.config;

import es.in2.wallet.vault.config.properties.hashicorp.HashiCorpVaultProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AppConfig {

    private final HashiCorpVaultProperties hashiCorpVaultProperties;

    @PostConstruct
    void init() {
        log.debug("Configurations uploaded: ");
        log.debug("HashiCorp Vault Properties: {}", hashiCorpVaultProperties);
    }

}
