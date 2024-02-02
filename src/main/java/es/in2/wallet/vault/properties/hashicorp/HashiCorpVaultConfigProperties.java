package es.in2.wallet.vault.properties.hashicorp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.Optional;


/**
 * HashiCorpVaultProperties
 *
 * @param authentication - authentication
 * @param token          - token
 * @param scheme         - scheme
 * @param host           - host
 * @param port           - port
 */
@ConfigurationProperties("spring.cloud.vault")
public record HashiCorpVaultConfigProperties(String authentication, String token, String scheme,
                                       String host, int port) {
    @ConstructorBinding
    public HashiCorpVaultConfigProperties(String authentication, String token, String scheme,
                                    String host, int port) {
        this.authentication = Optional.ofNullable(authentication).orElse("token");
        this.token = Optional.ofNullable(token).orElse("hvs.IcpenQulHxdxbqImRcW0WwrU");
        this.scheme = Optional.ofNullable(scheme).orElse("http");
        this.host = Optional.ofNullable(host).orElse("vault");
        this.port = Optional.of(port).orElse(8201);
    }
}
