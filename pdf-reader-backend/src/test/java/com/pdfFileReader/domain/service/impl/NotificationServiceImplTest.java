package com.pdfFileReader.domain.service.impl;

import com.pdfFileReader.domain.dto.ContractAnalysisResponse;
import com.pdfFileReader.domain.entity.Notification;
import com.pdfFileReader.testutil.TestPdfFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationServiceImplTest {

    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl("tesseract", "tessdata", "tur", "tur+eng", 300, null);
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

    @Test
    void repairTurkishTextRestoresTurkishDiacritics() {
        String ocrLikeText = "Sozlesme tarihi 15.03.2025. Mudurlugune bildirim yapilacaktir. "
                + "Gecici kabul 30.06.2025 gunu. Isin suresi 90 gun. Yururluge giris 1 Subat 2025.";

        String repaired = service.repairTurkishText(ocrLikeText);

        assertTrue(repaired.contains("Sözleşme"), "sozlesme -> sözleşme");
        assertTrue(repaired.contains("Müdürlüğüne"), "mudurlugune -> müdürlüğüne");
        assertTrue(repaired.contains("yapılacaktır"), "yapilacaktir -> yapılacaktır");
        assertTrue(repaired.contains("Geçici"), "gecici -> geçici (büyük harf korunur)");
        assertTrue(repaired.contains("günü"), "gunu -> günü");
        assertTrue(repaired.contains("süresi"), "suresi -> süresi");
        assertTrue(repaired.contains("Yürürlüğe"), "yururluge -> yürürlüğe");
        assertTrue(repaired.contains("Şubat"), "subat -> şubat");
        assertFalse(repaired.contains("Sozlesme"), "aksansız hali kalmamalı");
    }

    @Test
    void repairTurkishTextKeepsUnlistedWordsUntouched() {
        String text = "Herhangi bilinmeyen kelimeler aynen kalir. 12345";

        String repaired = service.repairTurkishText(text);

        assertEquals(text, repaired);
    }

    @Test
    void extractNotificationsIgnoresSectionNumbersAndCableSpecs() {
        String text = String.join("\n",
                "3.2.1.10.5. Yüklenici, İş'in ifasının tamamlanmasına kadar Saha'da HSE",
                "x150 8/35 mm2 20.3/35 kV Dahili AYE3SV,Kab.Bağl.-AL",
                "3.2.1.11. İşçilik, malzeme, ekipman, araç, makine, vinç, yakıt, yağ, sarf malzemesi,",
                "x150 5/35 mm2 20.3/35 kV Harici AYESSV, Kab. Başl.-AL | AD. [12,00]"
        ) + "\n";

        List<Notification> notifications = service.extractNotificationsFromText(text);

        assertTrue(notifications.isEmpty(),
                "numara/spec satirlari bildirim uretmemeli, uretilen: " + notifications);
    }

    @Test
    void extractNotificationsKeepsRealDatesInNoisyText() {
        String text = String.join("\n",
                "14. İşbu Protokol 20.10.2025 tarihinde Taraflar'ın karşılıklı ve birbirine uygun",
                "akdetmişlerdir. İşbu Protokol tahtında Taraflar, aşağıda yer alan şartların aralarında 20.10.2025",
                "Tarih : 20.10.2025",
                "Sözleşme tarihi: 20.10.2025.",
                "15/10/2025 tarihli"
        ) + "\n";

        List<Notification> notifications = service.extractNotificationsFromText(text);

        assertFalse(notifications.isEmpty(), "gercek tarihler bulunamadi");

        Set<LocalDate> distinctDates = notifications.stream()
                .map(Notification::getDueDate)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of(LocalDate.of(2025, 10, 20), LocalDate.of(2025, 10, 15)),
                distinctDates, "yanlis tarih seti cikarildi: " + notifications);
    }

    @Test
    void extractNotificationsBuildsDescriptionFromParagraph() {
        String text = String.join("\n",
                "Yüklenici iş programına göre çalışacaktır ve",
                "işi 31.05.2025 tarihinde tamamlayacaktır.",
                "",
                "Tek satırlık paragraf 30.09.2025."
        ) + "\n";

        List<Notification> notifications = service.extractNotificationsFromText(text);

        Notification paragraphNote = notifications.stream()
                .filter(n -> n.getDueDate().equals(LocalDate.of(2025, 5, 31)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("paragraf bildirimi bulunamadi: " + notifications));

        assertEquals(
                "Yüklenici iş programına göre çalışacaktır ve işi 31.05.2025 tarihinde tamamlayacaktır.",
                paragraphNote.getDescription(),
                "paragraf basligi aciklama olarak yazilmali"
        );

        Notification singleLineNote = notifications.stream()
                .filter(n -> n.getDueDate().equals(LocalDate.of(2025, 9, 30)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("tek satir bildirimi bulunamadi: " + notifications));
        assertNull(singleLineNote.getDescription(), "tek satirlik paragrafta aciklama olmamali");
    }

    @Test
    void extractNotificationsAcceptsCommaAfterTurkishMonth() {
        String text = "Kesin kabul 1 Aralik, 2026 tarihinde yapilacaktir.\n";

        List<Notification> notifications = service.extractNotificationsFromText(text);

        assertEquals(1, notifications.size());
        assertEquals(LocalDate.of(2026, 12, 1), notifications.get(0).getDueDate());
    }
}
