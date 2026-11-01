package com.portfolio.notificationservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record NotificationEmailDto(
        @NotBlank @Email String destinataire,
        @NotBlank  String objet,
        @NotBlank String contenu
) {
}
