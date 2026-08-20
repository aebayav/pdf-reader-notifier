package com.pdfFileReader.testutil;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.mock.web.MockMultipartFile;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

/**
 * Testler icin gercek (metin katmanli) PDF ureten yardimci.
 * Standart PDF fontlari Turkce karakter desteklemedigi icin
 * test metinleri ASCII karsiliklar kullanir (sozlesme, gecici kabul vs.).
 */
public final class TestPdfFactory {

    private TestPdfFactory() {
    }

    public static MockMultipartFile createPdf(String fileName, String... lines) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700);

                for (String line : lines) {
                    content.showText(line);
                    content.newLineAtOffset(0, -20);
                }

                content.endText();
            }

            document.save(out);
        }

        return new MockMultipartFile("file", fileName, "application/pdf", out.toByteArray());
    }

    /**
     * Metin katmani OLMAYAN (taranmis belge taklidi) PDF uretir.
     * Metin once AWT ile goruntuye cizilir (Arial Turkce karakterleri destekler),
     * sonra PDF'e gomulur. Boylece PDFTextStripper bos doner ve OCR devreye girer.
     */
    public static MockMultipartFile createImageOnlyPdf(String fileName, String... lines) throws Exception {
        int width = 1240;
        int height = Math.max(600, 120 + lines.length * 90);
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
            y += 90;
        }
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(595, 842)); // A4
            document.addPage(page);
            PDImageXObject ximage = LosslessFactory.createFromImage(document, pageImage);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.drawImage(ximage, 0, 0, page.getMediaBox().getWidth(), page.getMediaBox().getHeight());
            }
            document.save(out);
        }

        return new MockMultipartFile("file", fileName, "application/pdf", out.toByteArray());
    }
}
