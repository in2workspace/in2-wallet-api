package es.in2.wallet.api.facade.impl;

import com.fasterxml.jackson.core.JsonParseException;
import es.in2.wallet.api.facade.CredentialPresentationForTurnstileServiceFacade;
import es.in2.wallet.api.model.CredentialsBasicInfo;
import es.in2.wallet.api.service.CborGenerationService;
import es.in2.wallet.api.service.PresentationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.text.ParseException;



@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialPresentationForTurnstileServiceFacadeImpl implements CredentialPresentationForTurnstileServiceFacade {
    private final PresentationService presentationService;
    private final CborGenerationService cborGenerationService;

    @Override
    public Mono<String> createVerifiablePresentationForTurnstile(String processId, String authorizationToken, CredentialsBasicInfo credentialsBasicInfo) {
        return generateAudience()
                .flatMap(audience -> presentationService.createSignedVerifiablePresentation(processId, authorizationToken, credentialsBasicInfo, credentialsBasicInfo.id(), audience)
                )
                .flatMap(vp -> {
                    try {
                        return cborGenerationService.generateCbor(processId, vp);
                    } catch (ParseException e) {
                        return Mono.error(new JsonParseException("Error parsing the Verifiable Presentation"));
                    }
                });
    }

    private static Mono<String> generateAudience() {
        return Mono.just("vpTurnstile");
    }
}
