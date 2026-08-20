package com.pdfFileReader.domain.service.impl;

import com.pdfFileReader.testutil.TestPdfFactory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Turkce karakterlerin OCR pipeline'inda bozulmadigini dogrulayan regresyon testi.
 * Tesseract kurulu degilse test atlanir (CI'da guvenli).
 */
class OcrTurkishTextTest {

    private static final String TESSERACT = "C:\\Program Files\\Tesseract-OCR\\tesseract.exe";
    private static final String TESSDATA = "C:\\Users\\abaya\\tesseract-data\\tessdata";

    @Test
    void ocrPreservesTurkishDiacritics() throws Exception {
        Assumptions.assumeTrue(Files.exists(Path.of(TESSERACT)), "Tesseract kurulu degil, OCR testi atlandi");
        Assumptions.assumeTrue(Files.exists(Path.of(TESSDATA, "tur.traineddata")), "tessdata_best yok, OCR testi atlandi");

        NotificationServiceImpl service = new NotificationServiceImpl(
                TESSERACT, TESSDATA, "tur", "tur+eng", 300, null);

        MockMultipartFile pdf = TestPdfFactory.createImageOnlyPdf("taranmis-sozlesme.pdf",
                "Sözleşme tarihi 15 Mart 2025.",
                "Ücret çok yüksek, yüzde on beş indirim.",
                "İşin süresi doksan gündür.",
                "Geçici kabul müdürlükçe bildirilir."
        );

        String text = service.extractText(pdf);

        assertFalse(text.isBlank(), "OCR bos sonuc dondurdu");
        assertTrue(text.contains("Sözleşme"), "sozlesme kelimesi aksanli cikarilmali, cikti: " + text);
        assertTrue(text.contains("Ücret") || text.contains("ücret"), "ucret aksanli cikarilmali, cikti: " + text);
        assertTrue(text.contains("süresi") || text.contains("süre"), "sure aksanli cikarilmali, cikti: " + text);
        assertTrue(text.contains("gün") || text.contains("gündür"), "gun aksanli cikarilmali, cikti: " + text);
        assertTrue(text.contains("yüzde"), "yuzde aksanli cikarilmali, cikti: " + text);
        assertTrue(text.contains("Mart"), "ay adi okunamadi, cikti: " + text);
        assertFalse(text.contains("�"), "cozulemeyen karakter var, cikti: " + text);
    }
}
