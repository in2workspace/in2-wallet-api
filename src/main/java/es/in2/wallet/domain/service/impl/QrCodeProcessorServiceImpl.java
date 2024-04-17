package es.in2.wallet.domain.service.impl;

import es.in2.wallet.application.service.DomeAttestationExchangeService;
import es.in2.wallet.application.service.EbsiCredentialService;
import es.in2.wallet.domain.exception.NoSuchQrContentException;
import es.in2.wallet.application.service.AttestationExchangeService;
import es.in2.wallet.application.service.CredentialIssuanceService;
import es.in2.wallet.domain.model.QrType;
import es.in2.wallet.domain.service.QrCodeProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static es.in2.wallet.domain.model.QrType.*;
import static es.in2.wallet.domain.util.MessageUtils.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class QrCodeProcessorServiceImpl implements QrCodeProcessorService {

    private final CredentialIssuanceService credentialIssuanceService;
    private final EbsiCredentialService ebsiCredentialService;
    private final AttestationExchangeService attestationExchangeService;
    private final DomeAttestationExchangeService domeAttestationExchangeService;

    @Override
    public Mono<Object> processQrContent(String processId, String authorizationToken, String qrContent) {
        log.debug("ProcessID: {} - Processing QR content: {}", processId, qrContent);
        return identifyQrContentType(qrContent)
                .flatMap(qrType -> {
                    switch (qrType) {
                        case CREDENTIAL_OFFER_URI, OPENID_CREDENTIAL_OFFER: {
                            log.info("ProcessID: {} - Processing a Verifiable Credential Offer URI", processId);
                            return credentialIssuanceService.identifyAuthMethod(processId, authorizationToken, qrContent)
                                    .doOnSuccess(credential -> log.info("ProcessID: {} - Credential Issued: {}", processId, credential))
                                    .doOnError(e -> log.error("ProcessID: {} - Error while issuing credential: {}", processId, e.getMessage()));
                        }
                        case EBSI_CREDENTIAL_OFFER: {
                            log.info("ProcessID: {} - Processing a Verifiable Credential Offer URI in EBSI Format", processId);
                            return ebsiCredentialService.identifyAuthMethod(processId, authorizationToken, qrContent)
                                    .doOnSuccess(credential -> log.info("ProcessID: {} - Credential Issued: {}", processId, credential))
                                    .doOnError(e -> log.error("ProcessID: {} - Error while issuing credential: {}", processId, e.getMessage()));
                        }
                        case VC_LOGIN_REQUEST: {
                            log.info("ProcessID: {} - Processing a Verifiable Credential Login Request", processId);
                            return attestationExchangeService.processAuthorizationRequest(processId, authorizationToken, qrContent)
                                    .doOnSuccess(credential -> log.info("ProcessID: {} - Attestation Exchange", processId))
                                    .doOnError(e -> log.error("ProcessID: {} - Error while processing Attestation Exchange: {}", processId, e.getMessage()));
                        }
                        case OPENID_AUTHENTICATION_REQUEST: {
                            log.info("ProcessID: {} - Processing an Authentication Request", processId);
                            return Mono.error(new NoSuchQrContentException("OpenID Authentication Request not implemented yet"));
                        }
                        case DOME_VC_LOGIN_REQUEST: {
                            log.info("ProcessID: {} - Processing an Authentication Request from DOME", processId);
                            return domeAttestationExchangeService.getSelectableCredentialsRequiredToBuildThePresentation(processId,authorizationToken,qrContent);
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

    private Mono<QrType> identifyQrContentType(String qrContent) {
        return Mono.fromSupplier(() -> {
            if(DOME_LOGIN_REQUEST_PATTERN.matcher(qrContent).matches()){
                return DOME_VC_LOGIN_REQUEST;
            }
            else if (LOGIN_REQUEST_PATTERN.matcher(qrContent).matches()) {
                return VC_LOGIN_REQUEST;
            } else if (CREDENTIAL_OFFER_PATTERN.matcher(qrContent).matches()) {
                return QrType.CREDENTIAL_OFFER_URI;
            } else if (EBSI_CREDENTIAL_OFFER_PATTERN.matcher(qrContent).matches()){
                return EBSI_CREDENTIAL_OFFER;
            } else if (OPENID_CREDENTIAL_OFFER_PATTERN.matcher(qrContent).matches()) {
                return OPENID_CREDENTIAL_OFFER;
            } else if (OPENID_AUTHENTICATION_REQUEST_PATTERN.matcher(qrContent).matches()) {
                return OPENID_AUTHENTICATION_REQUEST;
            } else {
                log.warn("Unknown QR content type: {}", qrContent);
                return UNKNOWN;
            }
        });
    }


}
