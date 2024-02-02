package es.in2.wallet.configuration;

import es.in2.wallet.vault.properties.hashicorp.HashiCorpVaultConfigProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AppConfigs {

    private final HashiCorpVaultConfigProperties hashiCorpVaultProperties;


    @PostConstruct
    void init() {
        log.debug("Configurations uploaded: ");
        log.debug("HashiCorp Vault Properties: {}", hashiCorpVaultProperties);
    }

}