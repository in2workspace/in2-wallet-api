package es.in2.wallet.api.controller;

import es.in2.wallet.api.config.SwaggerConfig;
import es.in2.wallet.api.model.QrContent;
import es.in2.wallet.api.service.QrCodeProcessorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static es.in2.wallet.api.util.ApplicationUtils.getCleanBearerToken;

@Slf4j
@RestController
@RequestMapping("/api/v2/execute-content")
@RequiredArgsConstructor
public class QrCodeProcessorController {

    private final QrCodeProcessorService qrCodeProcessorService;

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
                .flatMap(authorizationToken -> qrCodeProcessorService.processQrContent(processId, authorizationToken, qrContent.content()));
    }

}
