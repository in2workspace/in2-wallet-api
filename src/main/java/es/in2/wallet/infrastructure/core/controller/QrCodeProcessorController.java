package es.in2.wallet.infrastructure.core.controller;

import es.in2.wallet.infrastructure.core.config.SwaggerConfig;
import es.in2.wallet.application.dto.QrContent;
import es.in2.wallet.application.workflows.processor.QrCodeProcessorWorkflow;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static es.in2.wallet.domain.utils.ApplicationUtils.getCleanBearerToken;

@Slf4j
@RestController
@RequestMapping("/api/v1/execute-content")
@RequiredArgsConstructor
public class QrCodeProcessorController {

    private final QrCodeProcessorWorkflow qrCodeProcessorWorkflow;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            tags = (SwaggerConfig.TAG_PUBLIC)
    )
    public Mono<Object> executeQrContent(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                                         @RequestBody QrContent qrContent) {
        String processId = UUID.randomUUID().toString();
        MDC.put("processId", processId);
        log.info("ProcessID: {} - Executing QR content: {}", processId, qrContent);
        return getCleanBearerToken(authorizationHeader)
                .flatMap(authorizationToken -> qrCodeProcessorWorkflow.processQrContent(processId, authorizationToken, qrContent.content()));
    }

}
