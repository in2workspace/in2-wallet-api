package es.in2.wallet.api.ebsi.comformance.controller;

import es.in2.wallet.api.ebsi.comformance.configuration.EbsiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v2/ebsi-did")
@RequiredArgsConstructor
public class EbsiDidController {

    private final EbsiConfig ebsiConfig;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Mono<String> getEbsiDid() {
        return ebsiConfig.getDid();
    }

}
