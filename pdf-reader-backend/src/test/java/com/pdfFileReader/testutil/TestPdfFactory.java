package com.pdfFileReader.testutil;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.mock.web.MockMultipartFile;

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
}
