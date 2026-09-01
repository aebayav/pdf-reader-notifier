package com.pdfFileReader.domain.service.impl;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.pdfFileReader.domain.dto.GeminiNote;
import com.pdfFileReader.domain.entity.Notification;
import com.pdfFileReader.domain.entity.Status;
import com.pdfFileReader.domain.service.GeminiService;
import com.pdfFileReader.domain.service.NotificationService;
import com.pdfFileReader.exception.GeminiException;
import com.pdfFileReader.repository.NotificationRepository;
import com.pdfFileReader.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GeminiServiceImpl implements GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiServiceImpl.class);

    /**
     * Prompt'a gonderilecek maksimum karakter sayisi. Gemini 3.6 Flash'in
     * 1M token'lik giris limiti vardir; sozlesmeler bu sinirin cok altinda
     * oldugu icin pratikte metin ASLA parcalanmaz/kisaltilmaz.
     */
    private static final int MAX_INPUT_CHARS = 1_000_000;

    private static final DateTimeFormatter ALTERNATE_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public GeminiServiceImpl(
            NotificationService notificationService,
            NotificationRepository notificationRepository,
            ObjectMapper objectMapper,
            @Value("${app.gemini.api-key:}") String apiKey,
            @Value("${app.gemini.model:gemini-3.6-flash}") String model,
            @Value("${app.gemini.url:https://generativelanguage.googleapis.com/v1beta/models}") String baseUrl
    ) {
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
    }

    @Override
    public List<Notification> analyzeAndCreateNotifications(MultipartFile file, UUID userId) {
        String resolvedKey = resolveApiKey();
        if (resolvedKey.isEmpty()) {
            throw new GeminiException(
                    "Gemini API anahtari bulunamadi. GEMINI_API_KEY ortam degiskeni ayarlayin "
                            + "veya proje kok dizinine .env dosyasi ekleyin (GEMINI_API_KEY=...)");
        }

        String text = notificationService.extractText(file);
        String sourceHash = HashUtil.sha256Hex(text);

        if (notificationRepository.existsBySourceHashAndUserId(sourceHash, userId)) {
            log.info("Bu belge bu kullanici tarafindan daha once AI ile islenmis, atlandi: {}",
                    file.getOriginalFilename());
            return List.of();
        }

        String prompt = buildPrompt(text);
        log.info("Gemini'ye TEK istek gonderiliyor: {} karakter (belgenin tamami)", prompt.length());
        String jsonText = callGemini(resolvedKey, prompt);
        List<Notification> notifications = parseToNotifications(jsonText);
        log.info("Gemini yaniti parcalara ayrildi: {} bildirim", notifications.size());

        if (notifications.isEmpty()) {
            log.warn("Gemini hicbir bildirim uretmedi: {}", file.getOriginalFilename());
        }

        for (Notification notification : notifications) {
            notification.setSourceHash(sourceHash);
            notification.setUserId(userId);
        }

        return notificationRepository.saveAll(notifications);
    }

    /**
     * Key'i soyle arar: ortam degiskeni (spring bunu zaten doldurur) ->
     * user.dir'e gore ./.env ve ../.env dosyalari. Boylece backend
     * IntelliJ'den, mvnw'den ya da run-backend.cmd'den baslatilsa da key bulunur.
     */
    String resolveApiKey() {
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey.trim();
        }

        for (Path candidate : List.of(Path.of(".env"), Path.of("../.env"))) {
            Path envFile = candidate.toAbsolutePath().normalize();
            String fromFile = readKeyFromEnvFile(envFile);
            if (!fromFile.isEmpty()) {
                log.info("Gemini API key .env dosyasindan okundu: {}", envFile);
                return fromFile;
            }
        }

        return "";
    }

    String readKeyFromEnvFile(Path envFile) {
        try {
            if (!Files.exists(envFile)) {
                return "";
            }

            for (String line : Files.readAllLines(envFile, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.startsWith("GEMINI_API_KEY=")) {
                    String value = trimmed.substring("GEMINI_API_KEY=".length()).trim();
                    return value.isEmpty() ? "" : value;
                }
            }

            return "";
        } catch (IOException e) {
            log.warn(".env dosyasi okunamadi: {}", envFile, e);
            return "";
        }
    }

    String buildPrompt(String text) {
        String trimmed = text.length() > MAX_INPUT_CHARS
                ? text.substring(0, MAX_INPUT_CHARS) + "\n... (metin kisaltildi)"
                : text;

        return """
                Sen bir sozlesme ve resmi yazi analiz asistanisin. Sana OCR ile taranmis bir belgeden cikarilan ham metni verecegim.
                Gorevin: Bu metinden, kullanicinin TAKIP ETMESI GEREKEN TARIHLI olaylari ve yukumlulukleri tespit etmek.
                Her olay icin su JSON'u uret:
                {"title": "kisa ve anlamli Turkce baslik", "description": "olayin ilgili cumlesi veya kisa ozeti", "dueDate": "YYYY-AA-GG"}
                Kurallar:
                - dueDate metinde gecen GERCEK bir tarih olmali; tarih belirsizse o maddeyi ATLA.
                - Ayni olayi birden fazla kez ekleme; mukerrer maddeleri birlestir.
                - Yanit YALNIZCA gecerli bir JSON dizisi olsun; kod blogu, markdown veya ek aciklama YAZMA.
                - En fazla 20 madde dondur.
                Metin:
                \"\"\"
                %s
                \"\"\"
                """.formatted(trimmed);
    }

    String callGemini(String key, String prompt) {
        try {
            URI uri = URI.create(baseUrl + "/" + model + ":generateContent?key=" + key);

            // Gemini REST govdesi: contents[].parts[].text + generationConfig
            Map<String, Object> payload = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", Map.of(
                            "responseMimeType", "application/json",
                            "temperature", 0.2
                    )
            );
            String body = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(120))
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                throw new GeminiException("Gemini API hatasi (" + response.statusCode() + "): "
                        + abbreviate(response.body(), 300));
            }

            JsonNode root = objectMapper.readTree(response.body());
            String text = root.path("candidates").path(0)
                    .path("content").path("parts").path(0)
                    .path("text").asText();

            if (text == null || text.isBlank()) {
                throw new GeminiException("Gemini bos yanit dondurdu.");
            }

            return text;
        } catch (GeminiException e) {
            throw e;
        } catch (Exception e) {
            throw new GeminiException("Gemini cagrisi basarisiz: " + e.getMessage(), e);
        }
    }

    List<Notification> parseToNotifications(String jsonText) {
        try {
            List<GeminiNote> notes = objectMapper.readValue(jsonText, new TypeReference<List<GeminiNote>>() {
            });

            List<Notification> notifications = new ArrayList<>();
            for (GeminiNote note : notes) {
                if (note == null || isBlank(note.title())) {
                    continue;
                }

                LocalDate dueDate = parseDate(note.dueDate());
                if (dueDate == null) {
                    log.warn("Gemini notu gecersiz tarih icerdigi icin atlandi: {} ({})", note.title(), note.dueDate());
                    continue;
                }

                Notification notification = new Notification();
                notification.setTitle(note.title().trim());
                notification.setDescription(isBlank(note.description()) ? null : note.description().trim());
                notification.setDueDate(dueDate);
                notification.setStatus(Status.IN_PROGRESS);
                notifications.add(notification);
            }

            return notifications;
        } catch (Exception e) {
            throw new GeminiException("Gemini yaniti JSON olarak ayrıştırılamadı: " + abbreviate(jsonText, 200), e);
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String cleaned = value.trim();
        try {
            return LocalDate.parse(cleaned);
        } catch (Exception ignored) {
            // alternatif: GG.AA.YYYY
        }

        try {
            return LocalDate.parse(cleaned, ALTERNATE_DATE);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String abbreviate(String value, int maxChars) {
        if (value == null) {
            return "null";
        }
        return value.length() > maxChars ? value.substring(0, maxChars) + "..." : value;
    }
}
