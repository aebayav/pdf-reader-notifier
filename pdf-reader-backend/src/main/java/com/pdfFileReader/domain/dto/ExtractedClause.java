package com.pdfFileReader.domain.dto;

import java.time.LocalDate;
import java.util.List;

public record ExtractedClause(
        String type,
        String text,
        List<LocalDate> dates,
        List<String> moneyAmounts,
        List<String> percentages
) {
}
