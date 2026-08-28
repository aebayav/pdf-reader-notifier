package com.pdfFileReader.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * KEY=value formatindaki ortam degiskenlerini okur: once gercek ortam
 * degiskenlerine, sonra user.dir'e gore ./.env ve ../.env dosyalarina bakar.
 * Boylece uygulama IntelliJ/mvnw/run-backend.cmd fark etmeksizin config bulur.
 */
public final class EnvReader {

    private EnvReader() {
    }

    public static String read(String key) {
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }

        for (Path candidate : List.of(Path.of(".env"), Path.of("../.env"))) {
            Path envFile = candidate.toAbsolutePath().normalize();
            try {
                if (!Files.exists(envFile)) {
                    continue;
                }
                for (String line : Files.readAllLines(envFile, StandardCharsets.UTF_8)) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith(key + "=")) {
                        String value = trimmed.substring(key.length() + 1).trim();
                        if (!value.isEmpty()) {
                            return value;
                        }
                    }
                }
            } catch (IOException ignored) {
                // dosya okunamazsa diger adaylara gecilir
            }
        }

        return "";
    }

    /** Enjekte edilen deger bossa .env/ortam degiskeninden okur. */
    public static String or(String injected, String key) {
        return (injected != null && !injected.isBlank()) ? injected.trim() : read(key);
    }
}
