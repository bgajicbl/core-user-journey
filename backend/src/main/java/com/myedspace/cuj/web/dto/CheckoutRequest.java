package com.myedspace.cuj.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotNull Long courseId,
        @NotNull @Email String parentEmail
) {
}
