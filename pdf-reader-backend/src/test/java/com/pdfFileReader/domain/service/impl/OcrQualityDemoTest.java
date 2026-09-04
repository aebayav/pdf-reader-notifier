package com.pdfFileReader.domain.service.impl;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * GECICI DEMO TESTI - commit edilmeyecek.
 * Eski OCR pipeline (200DPI RGB + tur+eng + fast tessdata) ile
 * yeni pipeline (300DPI GRAY + Otsu + tur + best tessdata + repair)
 * karsilastirmasi. Sonuclar dosyaya UTF-8 yazilir.
 */
class OcrQualityDemoTest {

    private static final String DEMO_OUT = System.getProperty("java.io.tmpdir") + "/ocr-demo-output.txt";

    @Test
    void compareOldAndNewPipeline() throws Exception {
        List<String> lines = List.of(
                "SÖZLEŞME",
                "Sözleşme tarihi 15 Mart 2025",
                "İşin süresi yer tesliminden itibaren 90 gündür.",
                "Yüklenici iş programına göre çalışacaktır.",
                "Gecikme cezası günlük %0,1 olarak uygulanır.",
                "Geçici kabul ve kesin kabul tarihleri müdürlükçe bildirilir.",
                "Ödeme tutarı 1.250.000 TL'dir.",
                "Yürürlüğe giriş 1 Şubat 2026 tarihindedir."
        );

        byte[] scannedPdf = buildScannedPdf(lines);
        MockMultipartFile file = new MockMultipartFile("file", "sozlesme-tarama.pdf", "application/pdf", scannedPdf);

        String oldText = oldStyleOcr(scannedPdf);
        NotificationServiceImpl newService = new NotificationServiceImpl(
                "C:\\Program Files\\Tesseract-OCR\\tesseract.exe",
                "C:\\Users\\abaya\\tesseract-data\\tessdata",
                "tur", "tur+eng", 300, null);
        String newText = newService.extractText(file);

        long oldDiacritics = countDiacritics(oldText);
        long newDiacritics = countDiacritics(newText);

        StringBuilder report = new StringBuilder();
        report.append("===== ESKİ pipeline (200DPI RGB + tur+eng + fast tessdata) =====\n");
        report.append(oldText).append("\n");
        report.append("Diyakritik (üçğşıöÜÇĞŞİÖ) sayısı: ").append(oldDiacritics).append("\n\n");
        report.append("===== YENİ pipeline (300DPI GRAY + Otsu + tur + best + repair) =====\n");
        report.append(newText).append("\n");
        report.append("Diyakritik (üçğşıöÜÇĞŞİÖ) sayısı: ").append(newDiacritics).append("\n");

        Files.writeString(Path.of(DEMO_OUT), report.toString(), StandardCharsets.UTF_8);
    }

    private static long countDiacritics(String text) {
        String tracked = "üçğşıöÜÇĞŞİÖ";
        return text.chars().filter(c -> tracked.indexOf(c) >= 0).count();
    }

    /** Metin katmanı olmayan (taranmış belge taklidi) PDF üretir. */
    private static byte[] buildScannedPdf(List<String> lines) throws Exception {
        // PDFBox standart fontlari (WinAnsi) Turkce karakterleri desteklemedigi icin
        // metin once AWT ile goruntuye cizilir, sonra PDF'e gomulur.
        int width = 1240;
        int height = 1754;
        BufferedImage pageImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = pageImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.PLAIN, 40));
        int y = 100;
        for (String line : lines) {
            g.drawString(line, 80, y);
            y += 80;
        }
        g.dispose();

        ByteArrayOutputStream scanned = new ByteArrayOutputStream();
        try (PDDocument imageDoc = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(595, 842)); // A4
            imageDoc.addPage(page);
            PDImageXObject ximg = LosslessFactory.createFromImage(imageDoc, pageImage);
            try (PDPageContentStream cs = new PDPageContentStream(imageDoc, page)) {
                cs.drawImage(ximg, 0, 0, page.getMediaBox().getWidth(), page.getMediaBox().getHeight());
            }
            imageDoc.save(scanned);
        }
        return scanned.toByteArray();
    }

    /** Eski pipeline taklidi: 200 DPI RGB render -> PNG -> tesseract tur+eng (fast tessdata). */
    private static String oldStyleOcr(byte[] scannedPdf) throws Exception {
        try (PDDocument doc = Loader.loadPDF(scannedPdf)) {
            BufferedImage rgb = new PDFRenderer(doc).renderImageWithDPI(0, 200, ImageType.RGB);
            Path tmp = Files.createTempFile("ocr-old-", ".png");
            try {
                ImageIO.write(rgb, "png", tmp.toFile());
                Process p = new ProcessBuilder(
                        "C:\\Program Files\\Tesseract-OCR\\tesseract.exe",
                        tmp.toString(), "stdout",
                        "-l", "tur+eng",
                        "--tessdata-dir", "C:\\Program Files\\Tesseract-OCR\\tessdata"
                ).start();
                String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                p.waitFor();
                return out;
            } finally {
                Files.deleteIfExists(tmp);
            }
        }
    }
}
