package es.in2.wallet.application.workflows.processor.impl;

import es.in2.wallet.application.workflows.issuance.Oid4vciWorkflow;
import es.in2.wallet.application.workflows.issuance.CredentialIssuanceEbsiWorkflow;
import es.in2.wallet.application.workflows.presentation.Oid4vpWorkflow;
import es.in2.wallet.application.dto.QrType;
import es.in2.wallet.application.workflows.processor.QrCodeProcessorWorkflow;
import es.in2.wallet.domain.exceptions.NoSuchQrContentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static es.in2.wallet.application.dto.QrType.*;
import static es.in2.wallet.domain.utils.ApplicationRegexPattern.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class QrCodeProcessorWorkflowImpl implements QrCodeProcessorWorkflow {

    private final Oid4vciWorkflow oid4vciWorkflow;
    private final CredentialIssuanceEbsiWorkflow credentialIssuanceEbsiWorkflow;
    private final Oid4vpWorkflow oid4vpWorkflow;
    @Override
    public Mono<Object> processQrContent(String processId, String authorizationToken, String qrContent) {
        log.debug("ProcessID: {} - Processing QR content: {}", processId, qrContent);
        return identifyOid4vcFlow(qrContent)
                .flatMap(qrType -> {
                    switch (qrType) {
                        case CREDENTIAL_OFFER_URI, OPENID_CREDENTIAL_OFFER: {
                            log.info("ProcessID: {} - Processing a Verifiable Credential Offer URI", processId);
                            return oid4vciWorkflow.execute(processId, authorizationToken, qrContent)
                                    .doOnSuccess(credential -> log.info("ProcessID: {} - Credential Issued: {}", processId, credential))
                                    .doOnError(e -> log.error("ProcessID: {} - Error while issuing credential: {}", processId, e.getMessage()));
                        }
                        case EBSI_CREDENTIAL_OFFER: {
                            log.info("ProcessID: {} - Processing a Verifiable Credential Offer URI in EBSI Format", processId);
                            return credentialIssuanceEbsiWorkflow.execute(processId, authorizationToken, qrContent)
                                    .doOnSuccess(credential -> log.info("ProcessID: {} - Credential Issued: {}", processId, credential))
                                    .doOnError(e -> log.error("ProcessID: {} - Error while issuing credential: {}", processId, e.getMessage()));
                        }
                        case VP_TOKEN_AUTHENTICATION_REQUEST: {
                            log.info("ProcessID: {} - Processing a Verifiable Credential Login Request for common workflow", processId);
                            return oid4vpWorkflow.processAuthorizationRequest(processId, authorizationToken, qrContent)
                                    .doOnSuccess(credential -> log.info("ProcessID: {} - Attestation Exchange", processId))
                                    .doOnError(e -> log.error("ProcessID: {} - Error while processing Attestation Exchange: {}", processId, e.getMessage()));

                        }
                        case UNKNOWN: {
                            String errorMessage = "The received QR content cannot be processed";
                            log.warn(errorMessage);
                            return Mono.error(new NoSuchQrContentException(errorMessage));
                        }
                        default: {
                            return Mono.empty();
                        }
                    }
                });
    }

    private Mono<QrType> identifyOid4vcFlow(String qrContent) {
        return Mono.fromSupplier(() -> {
            if(OPENID_VP_TOKEN_AUTHENTICATION_REQUEST_PATTERN.matcher(qrContent).matches()){
                return VP_TOKEN_AUTHENTICATION_REQUEST;
            } else if (OPENID_CREDENTIAL_OFFER_PATTERN.matcher(qrContent).matches()) {
                return OPENID_CREDENTIAL_OFFER;
            }  else {
                log.warn("Unknown QR content type: {}", qrContent);
                return UNKNOWN;
            }
        });
    }


}
