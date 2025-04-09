package es.in2.wallet.application.workflows.processor.impl;

import es.in2.wallet.application.workflows.issuance.CredentialIssuanceCommonWorkflow;
import es.in2.wallet.application.workflows.issuance.CredentialIssuanceEbsiWorkflow;
import es.in2.wallet.application.workflows.presentation.AttestationExchangeCommonWorkflow;
import es.in2.wallet.application.dto.QrType;
import es.in2.wallet.application.workflows.processor.QrCodeProcessorWorkflow;
import es.in2.wallet.infrastructure.appconfiguration.exception.WalletUnavailableException;
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

    private final CredentialIssuanceCommonWorkflow credentialIssuanceCommonWorkflow;
    private final CredentialIssuanceEbsiWorkflow credentialIssuanceEbsiWorkflow;
    private final AttestationExchangeCommonWorkflow attestationExchangeCommonWorkflow;
    @Override
    public Mono<Object> processQrContent(String processId, String authorizationToken, String qrContent) {
        log.debug("ProcessID: {} - Processing QR content: {}", processId, qrContent);
        return identifyQrContentType(qrContent)
                .flatMap(qrType -> {
                    switch (qrType) {
                        case CREDENTIAL_OFFER_URI, OPENID_CREDENTIAL_OFFER: {
                            log.info("ProcessID: {} - Processing a Verifiable Credential Offer URI", processId);
                            return credentialIssuanceCommonWorkflow.identifyAuthMethod(processId, authorizationToken, qrContent)
                                    .doOnSuccess(credential -> log.info("ProcessID: {} - Credential Issued: {}", processId, credential))
                                    .doOnError(e -> log.error("ProcessID: {} - Error while issuing credential: {}", processId, e.getMessage()));
                        }
                        case EBSI_CREDENTIAL_OFFER: {
                            log.info("ProcessID: {} - Processing a Verifiable Credential Offer URI in EBSI Format", processId);
                            return credentialIssuanceEbsiWorkflow.identifyAuthMethod(processId, authorizationToken, qrContent)
                                    .doOnSuccess(credential -> log.info("ProcessID: {} - Credential Issued: {}", processId, credential))
                                    .doOnError(e -> log.error("ProcessID: {} - Error while issuing credential: {}", processId, e.getMessage()));
                        }
                        case VP_TOKEN_AUTHENTICATION_REQUEST: {
                            log.info("ProcessID: {} - Processing a Verifiable Credential Login Request for common workflow", processId);
                            return attestationExchangeCommonWorkflow.processAuthorizationRequest(processId, authorizationToken, qrContent)
                                    .doOnSuccess(credential -> log.info("ProcessID: {} - Attestation Exchange", processId))
                                    .doOnError(e -> log.error("ProcessID: {} - Error while processing Attestation Exchange: {}", processId, e.getMessage()));

                        }
                        case UNKNOWN: {
                            String errorMessage = "Wallet unavailable: unsupported or unrecognized QR content";
                            log.warn("ProcessID: {} - {}", processId, errorMessage);
                            return Mono.error(new WalletUnavailableException(errorMessage));
                        }
                        default: {
                            return Mono.empty();
                        }
                    }
                });
    }

    private Mono<QrType> identifyQrContentType(String qrContent) {
        return Mono.fromSupplier(() -> {
            if(VP_TOKEN_AUTHENTICATION_REQUEST_PATTERN.matcher(qrContent).matches() || OPENID_VP_TOKEN_AUTHENTICATION_REQUEST_PATTERN.matcher(qrContent).matches()){
                return VP_TOKEN_AUTHENTICATION_REQUEST;
            }
            else if (CREDENTIAL_OFFER_PATTERN.matcher(qrContent).matches()) {
                return QrType.CREDENTIAL_OFFER_URI;
            } else if (EBSI_CREDENTIAL_OFFER_PATTERN.matcher(qrContent).matches()){
                return EBSI_CREDENTIAL_OFFER;
            } else if (OPENID_CREDENTIAL_OFFER_PATTERN.matcher(qrContent).matches()) {
                return OPENID_CREDENTIAL_OFFER;
            }  else {
                log.warn("Unknown QR content type: {}", qrContent);
                return UNKNOWN;
            }
        });
    }


}
