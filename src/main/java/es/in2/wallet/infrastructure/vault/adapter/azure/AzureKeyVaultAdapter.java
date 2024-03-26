package es.in2.wallet.infrastructure.vault.adapter.azure;

import com.azure.security.keyvault.secrets.SecretAsyncClient;
import es.in2.wallet.infrastructure.vault.model.VaultProviderEnum;
import es.in2.wallet.infrastructure.vault.model.KeyVaultSecret;
import es.in2.wallet.infrastructure.vault.service.GenericVaultService;
import es.in2.wallet.infrastructure.vault.util.VaultProviderAnnotation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Service
@Slf4j
@RequiredArgsConstructor
@VaultProviderAnnotation(provider = VaultProviderEnum.AZURE)
public class AzureKeyVaultAdapter implements GenericVaultService {
    private final SecretAsyncClient secretClient;

    @Override
    public Mono<Void> saveSecret(String key, KeyVaultSecret secret) {
        return secretClient.setSecret(sanitazeKey(key), secret.value().toString())
                .then()
                .doOnSuccess(voidValue -> log.info("Secret saved successfully"))
                .onErrorResume(Exception.class, e -> Mono.error(new RuntimeException("Error saving secret in Azure Key Vault", e)));
    }

    @Override
    public Mono<KeyVaultSecret> getSecret(String key) {
        return secretClient.getSecret(sanitazeKey(key))
                .flatMap(secret -> {
                    KeyVaultSecret keyVaultSecret = KeyVaultSecret.builder()
                            .value(secret.getValue())
                            .build();

                    return Mono.just(keyVaultSecret);
                })
                .doOnSuccess(secret -> log.info("Secret retrieved successfully"))
                .onErrorResume(Exception.class, Mono::error);
    }

    @Override
    public Mono<Void> deleteSecret(String key) {
        return  secretClient.beginDeleteSecret(sanitazeKey(key))
                .then()
                .doOnSuccess(voidValue -> log.info("Secret deleted successfully"))
                .onErrorResume(Exception.class, Mono::error);
    }

    private String sanitazeKey(String key) {
        return key.replace(":", "-");
    }

}
