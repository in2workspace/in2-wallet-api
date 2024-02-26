package es.in2.wallet.api.facade.impl;

import es.in2.wallet.api.facade.CredentialPresentationForTurnstileServiceFacade;
import es.in2.wallet.api.model.CredentialsBasicInfo;
import es.in2.wallet.api.service.CborGenerationService;
import es.in2.wallet.api.service.PresentationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialPresentationForTurnstileServiceFacadeImpl implements CredentialPresentationForTurnstileServiceFacade {
    private final PresentationService presentationService;
    private final CborGenerationService cborGenerationService;

    @Override
    public Mono<String> createVerifiablePresentationForTurnstile(String processId, String authorizationToken, CredentialsBasicInfo credentialsBasicInfo) {
        return generateNonce()
                .flatMap(nonce -> generateAudience()
                    .flatMap(audience -> presentationService.createSignedVerifiablePresentation(processId, authorizationToken, credentialsBasicInfo, nonce, audience)
                    )
                )
                .flatMap(vp -> cborGenerationService.generateCbor(processId, authorizationToken, vp));
    }

    private static Mono<String> generateNonce() {
        return Mono.fromCallable(() -> {
            UUID randomUUID = UUID.randomUUID();
            ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
            byteBuffer.putLong(randomUUID.getMostSignificantBits());
            byteBuffer.putLong(randomUUID.getLeastSignificantBits());
            byte[] uuidBytes = byteBuffer.array();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(uuidBytes);
        });
    }

    private static Mono<String> generateAudience() {
        return Mono.just("vpTurnstile");
    }
}
