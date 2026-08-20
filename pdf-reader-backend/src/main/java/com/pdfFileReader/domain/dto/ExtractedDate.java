package com.pdfFileReader.domain.dto;

import java.time.LocalDate;

public record ExtractedDate(
        LocalDate date,
        String sourceText
) {
}
