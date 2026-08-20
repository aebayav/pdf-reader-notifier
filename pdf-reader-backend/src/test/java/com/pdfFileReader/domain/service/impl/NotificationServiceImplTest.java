package com.pdfFileReader.domain.service.impl;

import com.pdfFileReader.domain.dto.ContractAnalysisResponse;
import com.pdfFileReader.testutil.TestPdfFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotificationServiceImplTest {

    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl("tesseract", "tessdata", "tur+eng", null);
    }

    @Test
    void analyzeContractFindsAllKeyContractFields() throws Exception {
        MockMultipartFile pdf = TestPdfFactory.createPdf("kontrat.pdf",
                "SOZLESME",
                "Sozlesme tarihi: 15.03.2025",
                "Baslangic tarihi: 01.04.2025",
                "Yer teslim tarihi: 10.04.2025",
                "Isin suresi yer tesliminden itibaren 90 gundur.",
                "Birim fiyat teklifi: 1.250,50 TL",
                "Gecikme cezasi gunluk %0,1 olarak uygulanir.",
                "Gecici kabul: 30.06.2025",
                "Kesin kabul: 30.09.2025",
                "Is programina gore 31.05.2025 tarihinde %50 tamamlanmis olacaktir."
        );

        ContractAnalysisResponse response = service.analyzeContract(pdf);

        assertNotNull(response.contractDate(), "sozlesme tarihi bulunamadi");
        assertEquals(LocalDate.of(2025, 3, 15), response.contractDate().date());

        assertNotNull(response.startDate(), "baslangic tarihi bulunamadi");
        assertEquals(LocalDate.of(2025, 4, 1), response.startDate().date());

        assertNotNull(response.siteDeliveryDate(), "yer teslim tarihi bulunamadi");
        assertEquals(LocalDate.of(2025, 4, 10), response.siteDeliveryDate().date());

        assertNotNull(response.durationAfterSiteDelivery(), "sure maddesi bulunamadi");

        assertFalse(response.unitPrices().isEmpty(), "birim fiyat bulunamadi");
        assertFalse(response.unitPrices().get(0).moneyAmounts().isEmpty(), "para tutari bulunamadi");

        assertFalse(response.penalties().isEmpty(), "ceza maddesi bulunamadi");
        assertFalse(response.penalties().get(0).percentages().isEmpty(), "ceza yuzdesi bulunamadi");

        assertNotNull(response.provisionalAcceptanceDate(), "gecici kabul tarihi bulunamadi");
        assertNotNull(response.finalAcceptanceDate(), "kesin kabul tarihi bulunamadi");
        assertEquals(LocalDate.of(2025, 9, 30), response.finalAcceptanceDate().date());

        assertFalse(response.progressMilestones().isEmpty(), "kilometre tasi bulunamadi");
        assertFalse(response.notifications().isEmpty(), "bildirim listesi bos");
    }

    @Test
    void analyzeContractParsesTurkishTextMonthDates() throws Exception {
        MockMultipartFile pdf = TestPdfFactory.createPdf("kontrat.pdf",
                "Bu sozlesme taraflar arasinda imzalanmis olup tum hukumleri taraflari baglayicidir.",
                "Kesin kabul 1 Aralik 2026 tarihinde yapilacaktir.",
                "Taraflar isbu sozlesmede yazili hukumler disinda herhangi bir hak talebinde bulunamaz."
        );

        ContractAnalysisResponse response = service.analyzeContract(pdf);

        assertNotNull(response.finalAcceptanceDate());
        assertEquals(LocalDate.of(2026, 12, 1), response.finalAcceptanceDate().date());
    }
}
