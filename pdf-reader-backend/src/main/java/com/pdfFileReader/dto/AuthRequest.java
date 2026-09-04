package com.pdfFileReader.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Giris/kayit istegi. */
public record AuthRequest(
        @NotBlank(message = "Kullanici adi bos olamaz.")
        @Size(min = 3, max = 64, message = "Kullanici adi 3-64 karakter olmali.")
        String username,

        @NotBlank(message = "Parola bos olamaz.")
        @Size(min = 6, max = 128, message = "Parola en az 6 karakter olmali.")
        String password,

        /** Opsiyonel; kayit sirasinda belirtilirse e-posta bildirimleri gonderilir. */
        @Email(message = "Gecerli bir e-posta adresi girin.")
        @Size(max = 255)
        String email
) {
}

