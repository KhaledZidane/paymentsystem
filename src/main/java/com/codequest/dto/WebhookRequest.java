package com.codequest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record WebhookRequest(
        @NotBlank(message = "paymentIntentId is required")
        String paymentIntentId,

        @NotBlank(message = "result is required")
        @Pattern(regexp = "CONFIRMED|FAILED", message = "result must be CONFIRMED or FAILED")
        String result
) {}
