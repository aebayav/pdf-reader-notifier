package com.pdfFileReader.domain.dto;

import com.pdfFileReader.domain.entity.Notification;

import java.util.List;

public record ContractAnalysisResponse(
        String fileName,
        String extractionMethod,
        ExtractedDate contractDate,
        ExtractedDate startDate,
        ExtractedDate siteDeliveryDate,
        ExtractedClause durationAfterSiteDelivery,
        List<ExtractedClause> unitPrices,
        ExtractedDate provisionalAcceptanceDate,
        ExtractedDate finalAcceptanceDate,
        List<ExtractedClause> progressMilestones,
        List<ExtractedClause> penalties,
        List<Notification> notifications
) {
}
