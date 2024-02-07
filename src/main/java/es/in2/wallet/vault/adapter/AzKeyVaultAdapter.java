package es.in2.wallet.vault.adapter;

import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.azure.security.keyvault.secrets.models.SecretProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.api.exception.ParseErrorException;
import es.in2.wallet.vault.service.GenericVaultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Map;

import static es.in2.wallet.api.util.MessageUtils.*;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "vault.provider.name", havingValue = "azure")
public class AzKeyVaultAdapter implements GenericVaultService {

    private final SecretClient secretClient;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> saveSecret(Map<String, String> secrets) {
        String processId = MDC.get(PROCESS_ID);
        try {
            String did = secrets.get("did"); // Use the DID as the key
            // Serialize secrets map to JSON or store individual components as needed
            String secretData = objectMapper.writeValueAsString(secrets);

            KeyVaultSecret newSecret = new KeyVaultSecret(
                    parseDidUriToAzureKeyVaultSecretName(did),
                    secretData)
                    .setProperties(new SecretProperties()
                            .setExpiresOn(OffsetDateTime.now().plusDays(60))
                            .setContentType("application/json"));

            return Mono.fromCallable(() ->
                            secretClient.setSecret(newSecret))
                    .then()
                    .doOnSuccess(voidValue -> log.info("ProcessID: {} - Secret saved successfully", processId))
                    .onErrorResume(Exception.class, Mono::error);
        }catch (Exception e) {
            return Mono.error(new ParseErrorException("Error while parsing secret data"));
        }
    }

    @Override
    public Mono<String> getSecretByKey(String key, String type) {
        String processId = MDC.get(PROCESS_ID);
        return Mono.fromCallable(() -> {
                    try {
                        KeyVaultSecret secret = secretClient.getSecret(parseDidUriToAzureKeyVaultSecretName(key));
                        Map<String, String> secretsMap = objectMapper.readValue(secret.getValue(), new TypeReference<>() {
                        });
                        if (type.equals(PRIVATE_KEY_TYPE)) {
                            return secretsMap.get(PRIVATE_KEY_TYPE);
                        } else if (type.equals(PUBLIC_KEY_TYPE)) {
                            return secretsMap.get(PUBLIC_KEY_TYPE);
                        } else {
                            throw new IllegalStateException("Invalid type");
                        }
                    } catch (Exception e) {
                        log.error("Communication with Key Vault failed: {}", e.getMessage(), e);
                        throw e;
                    }
                })
                .doOnSuccess(voidValue -> log.info("ProcessID: {} - Secret retrieved successfully", processId))
                .onErrorResume(Exception.class, Mono::error);
    }

    @Override
    public Mono<Void> deleteSecretByKey(String key) {
        String processId = MDC.get(PROCESS_ID);
        return Mono.fromRunnable(() -> {
                    try {
                        secretClient.beginDeleteSecret(parseDidUriToAzureKeyVaultSecretName(key));
                    } catch (Exception e) {
                        log.error("ProcessID: {} - Failed to delete secret: {}", processId, e.getMessage());
                    }
                })
                .then()
                .doOnSuccess(voidValue -> log.info("ProcessID: {} - Secret deleted successfully", processId))
                .onErrorResume(Exception.class, Mono::error);
    }

    private String parseDidUriToAzureKeyVaultSecretName(String key) {
        return key.replace(":", "-");
    }

}
