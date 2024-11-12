package es.in2.wallet.application.workflow.presentation.impl;

import es.in2.wallet.application.workflow.presentation.AttestationExchangeTurnstileWorkflow;
import es.in2.wallet.domain.model.CredentialsBasicInfo;
import es.in2.wallet.domain.service.CborGenerationService;
import es.in2.wallet.domain.service.PresentationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Slf4j
@Service
@RequiredArgsConstructor
public class AttestationExchangeTurnstileWorkflowImpl implements AttestationExchangeTurnstileWorkflow {
    private final PresentationService presentationService;
    private final CborGenerationService cborGenerationService;

    @Override
    public Mono<String> createVerifiablePresentationForTurnstile(String processId, String authorizationToken, CredentialsBasicInfo credentialsBasicInfo) {
        return generateAudience()
                .flatMap(audience -> presentationService.createSignedVerifiablePresentation(processId, authorizationToken, credentialsBasicInfo, credentialsBasicInfo.id(), audience)
                )
                .flatMap(vp -> cborGenerationService.generateCbor(processId, vp));
    }

    private static Mono<String> generateAudience() {
        return Mono.just("vpTurnstile");
    }
}
