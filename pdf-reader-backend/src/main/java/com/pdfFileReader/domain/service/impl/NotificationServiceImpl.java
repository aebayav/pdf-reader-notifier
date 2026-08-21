package com.pdfFileReader.domain.service.impl;

import com.pdfFileReader.domain.dto.ContractAnalysisResponse;
import com.pdfFileReader.domain.dto.ExtractedClause;
import com.pdfFileReader.domain.dto.ExtractedDate;
import com.pdfFileReader.domain.entity.Notification;
import com.pdfFileReader.domain.entity.Status;
import com.pdfFileReader.domain.service.NotificationService;
import com.pdfFileReader.exception.PdfReadException;
import com.pdfFileReader.repository.NotificationRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NotificationServiceImpl implements NotificationService {

    // Gürültüye karşı sertleştirilmiş: bölüm numaraları (3.2.1.10.5) ve kablo
    // spesifikasyonları (20.3/35) rakam/nokta bağlamında olduğu için elenir.
    // Ayırıcı sonrasındaki OCR kaynaklı boşluklara tolerans vardır.
    private static final Pattern NUMERIC_DATE_PATTERN = Pattern.compile(
            "(?<![\\d.])(\\d{1,2})[.\\-/]+\\s*(\\d{1,2})[.\\-/]+\\s*(\\d{2,4})(?!\\d)"
    );
    private static final Pattern TEXT_DATE_PATTERN = Pattern.compile(
            "(\\d{1,2})\\s+(ocak|subat|mart|nisan|mayis|haziran|temmuz|agustos|eylul|ekim|kasim|aralik)\\s*,?\\s*(\\d{4})"
    );
    private static final Pattern MONEY_PATTERN = Pattern.compile(
            "(?iu)(\\d{1,3}(?:[. ]\\d{3})*(?:,\\d{1,2})?|\\d+)(?:\\s)*(TL|TRY|USD|EUR|EURO|DOLAR|₺)"
    );
    private static final Pattern PERCENTAGE_PATTERN = Pattern.compile(
            "(?iu)(%\\s*\\d{1,3}|\\d{1,3}\\s*%)"
    );
    private static final Locale TURKISH = Locale.forLanguageTag("tr-TR");
    private static final Map<String, Integer> MONTHS = Map.ofEntries(
            Map.entry("ocak", 1),
            Map.entry("subat", 2),
            Map.entry("mart", 3),
            Map.entry("nisan", 4),
            Map.entry("mayis", 5),
            Map.entry("haziran", 6),
            Map.entry("temmuz", 7),
            Map.entry("agustos", 8),
            Map.entry("eylul", 9),
            Map.entry("ekim", 10),
            Map.entry("kasim", 11),
            Map.entry("aralik", 12)
    );

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    /**
     * OCR'un aksan isaretlerini dusurdugu yaygin sozlesme terimleri.
     * Sadece tam kelime eslesmeleri duzeltilir (kelime siniri korumali).
     */
    private static final Map<String, String> TURKISH_REPAIR_WORDS = Map.ofEntries(
            Map.entry("sozlesme", "sözleşme"),
            Map.entry("sozlesmesi", "sözleşmesi"),
            Map.entry("sozlesmenin", "sözleşmenin"),
            Map.entry("sozlesmeler", "sözleşmeler"),
            Map.entry("mudurluk", "müdürlük"),
            Map.entry("mudurlugu", "müdürlüğü"),
            Map.entry("mudurlugune", "müdürlüğüne"),
            Map.entry("mudurlukleri", "müdürlükleri"),
            Map.entry("gunluk", "günlük"),
            Map.entry("gun", "gün"),
            Map.entry("gunu", "günü"),
            Map.entry("gunler", "günler"),
            Map.entry("gunun", "günün"),
            Map.entry("sure", "süre"),
            Map.entry("suresi", "süresi"),
            Map.entry("sureyi", "süreyi"),
            Map.entry("surenin", "sürenin"),
            Map.entry("yuzde", "yüzde"),
            Map.entry("yil", "yıl"),
            Map.entry("yillik", "yıllık"),
            Map.entry("yilinda", "yılında"),
            Map.entry("odeme", "ödeme"),
            Map.entry("odemeler", "ödemeler"),
            Map.entry("odemenin", "ödemenin"),
            Map.entry("gecici", "geçici"),
            Map.entry("baslangic", "başlangıç"),
            Map.entry("yururluk", "yürürlük"),
            Map.entry("yururluge", "yürürlüğe"),
            Map.entry("yapilacak", "yapılacak"),
            Map.entry("yapilacaktir", "yapılacaktır"),
            Map.entry("olacaktir", "olacaktır"),
            Map.entry("tutari", "tutarı"),
            Map.entry("tutarinda", "tutarında"),
            Map.entry("isbu", "işbu"),
            Map.entry("isin", "işin"),
            Map.entry("isleri", "işleri"),
            Map.entry("isveren", "işveren"),
            Map.entry("hukum", "hüküm"),
            Map.entry("hukumleri", "hükümleri"),
            Map.entry("imzalanmis", "imzalanmış"),
            Map.entry("tamamlanmis", "tamamlanmış"),
            Map.entry("asagida", "aşağıda"),
            Map.entry("yukarida", "yukarıda"),
            Map.entry("subat", "şubat"),
            Map.entry("mayis", "mayıs"),
            Map.entry("kasim", "kasım"),
            Map.entry("aralik", "aralık"),
            Map.entry("eylul", "eylül"),
            Map.entry("agustos", "ağustos")
    );
    private static final Pattern TURKISH_REPAIR_PATTERN = Pattern.compile(
            "(?iu)\\b(" + String.join("|", TURKISH_REPAIR_WORDS.keySet()) + ")\\b"
    );

    private final String ocrExecutable;
    private final String ocrDatapath;
    private final String ocrPrimaryLanguage;
    private final String ocrFallbackLanguage;
    private final int ocrDpi;
    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(
            @Value("${app.ocr.executable:C:\\Program Files\\Tesseract-OCR\\tesseract.exe}") String ocrExecutable,
            @Value("${app.ocr.datapath:C:\\Program Files\\Tesseract-OCR\\tessdata}") String ocrDatapath,
            @Value("${app.ocr.primary-language:tur}") String ocrPrimaryLanguage,
            @Value("${app.ocr.fallback-language:tur+eng}") String ocrFallbackLanguage,
            @Value("${app.ocr.dpi:300}") int ocrDpi,
            NotificationRepository notificationRepository
    ) {
        this.ocrExecutable = ocrExecutable;
        this.ocrDatapath = ocrDatapath;
        this.ocrPrimaryLanguage = ocrPrimaryLanguage;
        this.ocrFallbackLanguage = ocrFallbackLanguage;
        this.ocrDpi = ocrDpi;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public List<Notification> processAndSaveNotifications(MultipartFile file) {
        ExtractedTextResult extractedText = extractReadableText(file);
        List<Notification> notifications = extractNotificationsFromText(extractedText.text());

        if (notifications.isEmpty()) {
            log.warn("No date-bearing notifications found in '{}'", file.getOriginalFilename());
        }

        // Aynı başlık + tarih kombinasyonu zaten kayıtlıysa mükerrer ekleme yapılmaz
        List<Notification> newNotifications = new ArrayList<>();
        for (Notification notification : notifications) {
            if (notificationRepository.existsByTitleAndDueDate(notification.getTitle(), notification.getDueDate())) {
                log.info("Zaten kayitli oldugu icin atlandi: {}", notification.getTitle());
                continue;
            }
            newNotifications.add(notification);
        }

        return notificationRepository.saveAll(newNotifications);
    }

    @Override
    public ContractAnalysisResponse analyzeContract(MultipartFile file) {
        ExtractedTextResult extractedText = extractReadableText(file);
        List<String> sections = splitIntoSections(extractedText.text());
        List<Notification> notifications = extractNotificationsFromText(extractedText.text());

        return new ContractAnalysisResponse(
                file.getOriginalFilename(),
                extractedText.method(),
                findDateByKeywords(sections, List.of("sozlesme tarihi", "sozlesme imza", "imza tarihi", "tanzim tarihi")),
                findDateByKeywords(sections, List.of("baslangic tarihi", "baslama tarihi", "ise baslama", "is baslama", "yururluk tarihi")),
                findDateByKeywords(sections, List.of("yer teslim tarihi", "yer teslimi", "yer teslim")),
                findDurationAfterSiteDelivery(sections),
                findClauses(sections, "UNIT_PRICE", List.of("birim fiyat", "birim bedel", "poz", "fiyat farki")),
                findDateByKeywords(sections, List.of("gecici kabul")),
                findDateByKeywords(sections, List.of("kesin kabul")),
                findProgressMilestones(sections),
                findClauses(sections, "PENALTY", List.of("cezai", "ceza", "mueyyide", "gecikme cezasi", "kesinti", "teminat irat")),
                notifications
        );
    }

    @Override
    public String extractText(MultipartFile file) {
        return extractReadableText(file).text();
    }

    private ExtractedTextResult extractReadableText(MultipartFile file) {
        String text = extractTextWithPdfBox(file);
        String method = "PDF_TEXT";

        if (isTextLowQuality(text)) {
            log.info("PDF text is low quality, falling back to OCR.");
            text = extractTextWithOcr(file);
            method = "OCR";
        }

        if (isTextLowQuality(text)) {
            throw new PdfReadException("PDF could not be read.");
        }

        return new ExtractedTextResult(text, method);
    }

    private String extractTextWithPdfBox(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(document);

            if (text == null || text.isBlank()) {
                log.warn("PDF text is empty or could not be read.");
                return "";
            }

            return text;
        } catch (IOException e) {
            throw new PdfReadException("PDF could not be read.", e);
        }
    }

    List<Notification> extractNotificationsFromText(String text) {
        List<String> lines = Arrays.asList(text.split("\\R"));
        List<Notification> results = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.trim().isEmpty()) {
                continue;
            }

            for (LocalDate date : extractDates(line, 2000, 2100)) {
                Notification notification = new Notification();
                notification.setTitle(buildNotificationTitle(lines, line, i));
                notification.setDescription(buildNotificationDescription(lines, i));
                notification.setDueDate(date);
                notification.setStatus(Status.IN_PROGRESS);
                results.add(notification);
            }
        }

        return results;
    }

    /**
     * Bildirimin içeriği: tarih satırının ait olduğu paragraf (boş satırla ayrılan blok).
     * Tek satırlık paragraflar için null döner (başlık yeterli).
     */
    private String buildNotificationDescription(List<String> lines, int index) {
        int start = index;
        while (start > 0 && !lines.get(start - 1).trim().isEmpty()) {
            start--;
        }

        int end = index;
        while (end < lines.size() - 1 && !lines.get(end + 1).trim().isEmpty()) {
            end++;
        }

        if (start == end) {
            return null;
        }

        StringBuilder paragraph = new StringBuilder();
        for (int k = start; k <= end; k++) {
            String part = lines.get(k).trim();
            if (!part.isEmpty()) {
                paragraph.append(part).append(' ');
            }
        }

        String cleaned = paragraph.toString().trim();
        return cleaned.length() > 600 ? cleaned.substring(0, 600) + "..." : cleaned;
    }

    private ExtractedDate findDateByKeywords(List<String> sections, List<String> keywords) {
        for (String section : sections) {
            if (!containsAny(section, keywords)) {
                continue;
            }

            List<LocalDate> dates = extractDates(section, 1900, 2200);
            if (!dates.isEmpty()) {
                return new ExtractedDate(dates.get(0), trimForResponse(section));
            }
        }

        return null;
    }

    private ExtractedClause findDurationAfterSiteDelivery(List<String> sections) {
        for (String section : sections) {
            String normalized = normalize(section);
            boolean mentionsDuration = normalized.contains("sure") || normalized.contains("gun");
            boolean mentionsSiteDelivery = normalized.contains("yer teslim") || normalized.contains("tesliminden");

            if (mentionsDuration && mentionsSiteDelivery) {
                return buildClause("DURATION_AFTER_SITE_DELIVERY", section);
            }
        }

        return null;
    }

    private List<ExtractedClause> findProgressMilestones(List<String> sections) {
        List<ExtractedClause> results = new ArrayList<>();

        for (String section : sections) {
            String normalized = normalize(section);
            boolean hasProgressKeyword = containsAnyNormalized(normalized, List.of(
                    "tamamlan", "bitiril", "bitmesi", "is programi", "termin", "kilometre tasi", "ilerleme"
            ));
            boolean hasQuantifier = !extractPercentages(section).isEmpty() || !extractDates(section, 1900, 2200).isEmpty();

            if (hasProgressKeyword && hasQuantifier) {
                results.add(buildClause("PROGRESS_MILESTONE", section));
            }
        }

        return distinctClauses(results);
    }

    private List<ExtractedClause> findClauses(List<String> sections, String type, List<String> keywords) {
        List<ExtractedClause> results = new ArrayList<>();

        for (String section : sections) {
            if (containsAny(section, keywords)) {
                results.add(buildClause(type, section));
            }
        }

        return distinctClauses(results);
    }

    private ExtractedClause buildClause(String type, String text) {
        return new ExtractedClause(
                type,
                trimForResponse(text),
                extractDates(text, 1900, 2200),
                extractMoneyAmounts(text),
                extractPercentages(text)
        );
    }

    private List<String> splitIntoSections(String text) {
        Set<String> sections = new LinkedHashSet<>();

        for (String paragraph : text.split("(\\R\\s*){2,}")) {
            addSection(sections, paragraph);
        }

        for (String line : text.split("\\R")) {
            addSection(sections, line);
        }

        return new ArrayList<>(sections);
    }

    private void addSection(Set<String> sections, String rawText) {
        String section = cleanWhitespace(rawText);
        if (!section.isBlank()) {
            sections.add(section);
        }
    }

    private List<ExtractedClause> distinctClauses(List<ExtractedClause> clauses) {
        Set<String> seen = new LinkedHashSet<>();
        List<ExtractedClause> results = new ArrayList<>();

        for (ExtractedClause clause : clauses) {
            if (seen.add(clause.text())) {
                results.add(clause);
            }
        }

        return results;
    }

    private String buildNotificationTitle(List<String> lines, String line, int index) {
        String title = line.trim();
        if (countLetters(title) < 5 && index > 0) {
            title = lines.get(index - 1).trim() + " | " + title;
        }

        return title;
    }

    private long countLetters(String value) {
        return value.chars()
                .filter(Character::isLetter)
                .count();
    }

    private List<LocalDate> extractDates(String text, int minYear, int maxYear) {
        Set<LocalDate> dates = new LinkedHashSet<>();
        Matcher numericMatcher = NUMERIC_DATE_PATTERN.matcher(text);

        while (numericMatcher.find()) {
            addDate(dates, numericMatcher.group(1), numericMatcher.group(2), numericMatcher.group(3), minYear, maxYear);
        }

        Matcher textMatcher = TEXT_DATE_PATTERN.matcher(normalize(text));
        while (textMatcher.find()) {
            Integer month = MONTHS.get(textMatcher.group(2));
            if (month != null) {
                addDate(dates, textMatcher.group(1), String.valueOf(month), textMatcher.group(3), minYear, maxYear);
            }
        }

        return new ArrayList<>(dates);
    }

    private void addDate(Set<LocalDate> dates, String dayValue, String monthValue, String yearValue, int minYear, int maxYear) {
        try {
            int day = Integer.parseInt(dayValue);
            int month = Integer.parseInt(monthValue);
            int year = Integer.parseInt(yearValue);

            if (year < 100) {
                year += 2000;
                // 2 haneli yıllar için makul olmayan geleceği eler
                // (ör. "20.3/35 kV" kablo spesifikasyonundaki "35" -> 2035)
                if (year > LocalDate.now().getYear() + 5) {
                    return;
                }
            }

            if (year < minYear || year > maxYear) {
                return;
            }

            dates.add(LocalDate.of(year, month, day));
        } catch (Exception ignored) {
        }
    }

    private List<String> extractMoneyAmounts(String text) {
        return extractMatches(MONEY_PATTERN, text);
    }

    private List<String> extractPercentages(String text) {
        return extractMatches(PERCENTAGE_PATTERN, text);
    }

    private List<String> extractMatches(Pattern pattern, String text) {
        Set<String> results = new LinkedHashSet<>();
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            results.add(matcher.group().trim());
        }

        return new ArrayList<>(results);
    }

    private boolean containsAny(String text, List<String> keywords) {
        return containsAnyNormalized(normalize(text), keywords);
    }

    private boolean containsAnyNormalized(String normalizedText, List<String> keywords) {
        for (String keyword : keywords) {
            if (normalizedText.contains(normalize(keyword))) {
                return true;
            }
        }

        return false;
    }

    private String normalize(String text) {
        String lower = text.toLowerCase(TURKISH)
                .replace('ı', 'i')
                .replace('İ', 'i');
        String normalized = Normalizer.normalize(lower, Normalizer.Form.NFD);

        return normalized.replaceAll("\\p{M}", "");
    }

    private String cleanWhitespace(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }

    private String trimForResponse(String text) {
        String cleaned = cleanWhitespace(text);
        if (cleaned.length() <= 1200) {
            return cleaned;
        }

        return cleaned.substring(0, 1200) + "...";
    }

    private String extractTextWithOcr(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFRenderer renderer = new PDFRenderer(document);
            renderer.setSubsamplingAllowed(true);

            StringBuilder text = new StringBuilder();

            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                // 300 DPI gri tonlama: Turkce aksanlarin dogru taninmasi icin.
                // Ek ikilestirme (Otsu vb.) yapilmaz; Tesseract 5 LSTM dahili esikleme
                // uygular ve on-ikilestirme ince isaretleri (%, ., virgul) bozar.
                BufferedImage grayImage = renderer.renderImageWithDPI(pageIndex, ocrDpi, ImageType.GRAY);

                String pageText = ocrPageWithFallback(grayImage, pageIndex);

                text.append(pageText).append(System.lineSeparator());
                grayImage.flush();
            }

            return repairTurkishText(text.toString());
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

    /**
     * Once birincil dil ile OCR dener; komut basarisiz olursa ya da sonuc
     * dusuk kaliteyse yedek dili dener ve harf sayisi fazla olani secer.
     */
    private String ocrPageWithFallback(BufferedImage image, int pageIndex)
            throws IOException, InterruptedException {
        String primaryText;
        try {
            primaryText = doOcrFromImageFile(image, pageIndex, ocrPrimaryLanguage);
        } catch (PdfReadException primaryFailure) {
            log.warn("Birincil OCR dili ({}) basarisiz: {}", ocrPrimaryLanguage, primaryFailure.getMessage());
            primaryText = null;
        }

        if (ocrFallbackLanguage.equalsIgnoreCase(ocrPrimaryLanguage)) {
            if (primaryText == null) {
                throw new PdfReadException("OCR could not read any text.");
            }
            return primaryText;
        }

        if (primaryText == null || isTextLowQuality(primaryText)) {
            log.info("Yedek OCR dili deneniyor: {}", ocrFallbackLanguage);
            String fallbackText = doOcrFromImageFile(image, pageIndex, ocrFallbackLanguage);

            if (primaryText == null || countLetters(fallbackText) > countLetters(primaryText)) {
                return fallbackText;
            }
        }

        return primaryText;
    }

    private String doOcrFromImageFile(BufferedImage image, int pageIndex, String language)
            throws IOException, InterruptedException {
        Path tempImage = Files.createTempFile("pdf-ocr-page-" + pageIndex + "-", ".png");
        // Cikti taban ismi .txt ile bitmemeli; tesseract zaten .txt uzantisi ekler.
        Path outputBase = Files.createTempFile("pdf-ocr-out-" + pageIndex + "-", "");
        Path outputFile = Path.of(outputBase + ".txt");

        try {
            boolean imageWritten = ImageIO.write(image, "png", tempImage.toFile());
            if (!imageWritten) {
                throw new IOException("PNG writer is not available.");
            }

            List<String> command = new ArrayList<>();
            command.add(ocrExecutable);
            command.add(tempImage.toString());
            command.add(outputBase.toString());
            command.add("-l");
            command.add(language);
            command.add("-c");
            command.add("preserve_interword_spaces=1");
            // Render DPI'ini acikca bildirir; aksi halde tesseract tahmin yurur
            // ("Estimating resolution as ...") ve tahminin sapmasi tanima kalitesini bozar.
            command.add("-c");
            command.add("user_defined_dpi=" + ocrDpi);

            if (ocrDatapath != null && !ocrDatapath.isBlank()) {
                command.add("--tessdata-dir");
                command.add(ocrDatapath);
            }

            Process process = new ProcessBuilder(command).start();
            String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new PdfReadException("Tesseract OCR command failed: " + error.trim());
            }

            if (!error.isBlank()) {
                // Basarili islemde stderr bilgilendirme amacli; log gurultusu olmamasi icin debug
                log.debug("Tesseract stderr: {}", error.trim());
            }

            // Dosya ciktisi her zaman UTF-8'dir. stdout Windows'ta konsol kod sayfasina
            // (CP1254 vb.) takilabildigi icin dosya ciktisi tercih edilir.
            return Files.readString(outputFile);
        } finally {
            Files.deleteIfExists(tempImage);
            Files.deleteIfExists(outputBase);
            Files.deleteIfExists(outputFile);
        }
    }

    /**
     * OCR ciktisinda aksanlari dusen yaygin sozlesme terimlerini duzeltir.
     */
    String repairTurkishText(String text) {
        Matcher matcher = TURKISH_REPAIR_PATTERN.matcher(text);
        StringBuilder repaired = new StringBuilder();

        while (matcher.find()) {
            String matched = matcher.group(1);
            String replacement = TURKISH_REPAIR_WORDS.get(normalize(matched.toLowerCase(TURKISH)));

            if (Character.isUpperCase(matched.charAt(0))) {
                replacement = replacement.substring(0, 1).toUpperCase(TURKISH) + replacement.substring(1);
            }

            matcher.appendReplacement(repaired, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(repaired);
        return repaired.toString();
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

    private record ExtractedTextResult(String text, String method) {
    }
}
