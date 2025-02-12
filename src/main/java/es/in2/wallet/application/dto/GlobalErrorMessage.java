package es.in2.wallet.application.dto;

import lombok.Builder;

@Builder
public record GlobalErrorMessage(String title, String message, String path) {

}
