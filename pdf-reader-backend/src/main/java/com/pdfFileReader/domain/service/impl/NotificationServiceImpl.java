package com.pdfFileReader.domain.service.impl;

import com.pdfFileReader.domain.entity.Notification;
import com.pdfFileReader.domain.entity.Status;
import com.pdfFileReader.domain.service.NotificationService;
import com.pdfFileReader.exception.PdfReadException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final String ocrExecutable;
    private final String ocrDatapath;
    private final String ocrLanguage;

    public NotificationServiceImpl(
            @Value("${app.ocr.executable:C:\\Program Files\\Tesseract-OCR\\tesseract.exe}") String ocrExecutable,
            @Value("${app.ocr.datapath:C:\\Program Files\\Tesseract-OCR\\tessdata}") String ocrDatapath,
            @Value("${app.ocr.language:tur+eng}") String ocrLanguage
    ) {
        this.ocrExecutable = ocrExecutable;
        this.ocrDatapath = ocrDatapath;
        this.ocrLanguage = ocrLanguage;
    }

    @Override
    public List<Notification> processAndSaveNotifications(MultipartFile file) {
        String text = extractText(file);

        if (isTextLowQuality(text)) {
            System.out.println("PDF text is low quality. Trying OCR.");
            text = extractTextWithOcr(file);
        }

        if (isTextLowQuality(text)) {
            throw new PdfReadException("PDF could not be read. OCR result is also insufficient.");
        }

        List<String> lines = Arrays.asList(text.split("\\R"));
        List<Notification> results = new ArrayList<>();

        System.out.println("--- PDF read started: " + file.getOriginalFilename() + " ---");
        for (int i = 0; i < lines.size(); i++) {
            System.out.println("Line " + (i + 1) + ": " + lines.get(i));
        }
        System.out.println("--- PDF read finished, analysis started ---");

        Pattern regexPattern = Pattern.compile("(\\d{2})[./-](\\d{2})[./-](\\d{4})");

        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }

            Matcher matcher = regexPattern.matcher(line);
            if (matcher.find()) {
                try {
                    int day = Integer.parseInt(matcher.group(1));
                    int month = Integer.parseInt(matcher.group(2));
                    int year = Integer.parseInt(matcher.group(3));

                    Notification notification = new Notification();
                    notification.setTitle(line.trim());
                    notification.setDueDate(LocalDate.of(year, month, day));
                    notification.setStatus(Status.IN_PROGRESS);

                    results.add(notification);
                    System.out.println(">> Match found: " + line.trim());
                } catch (Exception e) {
                    System.err.println("!! Date conversion failed: " + line);
                }
            }
        }

        return results;
    }

    @Override
    public String extractText(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(document);

            if (text == null || text.isBlank()) {
                System.out.println("PDF text is empty or could not be read.");
                return "";
            }

            return text;
        } catch (IOException e) {
            throw new PdfReadException("PDF could not be read.", e);
        }
    }

    private String extractTextWithOcr(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFRenderer renderer = new PDFRenderer(document);
            renderer.setSubsamplingAllowed(true);

            StringBuilder text = new StringBuilder();

            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                BufferedImage image = renderer.renderImageWithDPI(pageIndex, 200, ImageType.RGB);
                BufferedImage rgbImage = convertToRgbImage(image);

                String pageText = doOcrFromImageFile(rgbImage, pageIndex);

                text.append(pageText).append(System.lineSeparator());
                image.flush();
                rgbImage.flush();
            }

            return text.toString();
        } catch (IOException e) {
            throw new PdfReadException("PDF pages could not be rendered for OCR.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PdfReadException("OCR was interrupted.", e);
        } catch (PdfReadException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new PdfReadException("OCR failed while converting PDF image.", e);
        }
    }

    private String doOcrFromImageFile(BufferedImage image, int pageIndex)
            throws IOException, InterruptedException {
        Path tempImage = Files.createTempFile("pdf-ocr-page-" + pageIndex + "-", ".png");

        try {
            boolean imageWritten = ImageIO.write(image, "png", tempImage.toFile());
            if (!imageWritten) {
                throw new IOException("PNG writer is not available.");
            }

            List<String> command = new ArrayList<>();
            command.add(ocrExecutable);
            command.add(tempImage.toString());
            command.add("stdout");
            command.add("-l");
            command.add(ocrLanguage);

            if (ocrDatapath != null && !ocrDatapath.isBlank()) {
                command.add("--tessdata-dir");
                command.add(ocrDatapath);
            }

            Process process = new ProcessBuilder(command).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new PdfReadException("Tesseract OCR command failed: " + error.trim());
            }

            if (!error.isBlank()) {
                System.err.println(error.trim());
            }

            return output;
        } finally {
            Files.deleteIfExists(tempImage);
        }
    }

    private BufferedImage convertToRgbImage(BufferedImage source) {
        BufferedImage rgbImage = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D graphics = rgbImage.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, rgbImage.getWidth(), rgbImage.getHeight());
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();

        return rgbImage;
    }

    private boolean isTextLowQuality(String text) {
        if (text == null || text.isBlank()) {
            return true;
        }

        String trimmedText = text.trim();
        long letterCount = trimmedText.chars()
                .filter(Character::isLetter)
                .count();
        double letterRatio = (double) letterCount / trimmedText.length();

        return trimmedText.length() < 100 || letterRatio < 0.25;
    }
}
