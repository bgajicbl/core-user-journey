package com.myedspace.cuj.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OnboardingRequest(
        @NotBlank String studentName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 10, max = 72, message = "must be between 10 and 72 characters") String password
) {
}
