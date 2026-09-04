package com.pdfFileReader.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNotificationGroupRequest(
        @NotBlank(message = "Grup adi bos olamaz.")
        @Size(max = 100, message = "Grup adi en fazla 100 karakter olabilir.")
        String name
) {
}