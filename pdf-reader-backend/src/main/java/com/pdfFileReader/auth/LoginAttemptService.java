package com.pdfFileReader.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory brute-force koruması. Ayni IP/kullanici adi icin
 * MAX_ATTEMPTS baskisiz giris denemesini asinca LOCK_DURATION_MS
 * suresince yeni giris engellenir. Sunucu yeniden baslarsa sayaclar sifirlanir
 * – bu uygulama olcegi icin yeterlidir.
 */
@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    /** Kilitlenme oncesi izin verilen maksimum basarisiz giris denemesi. */
    private static final int MAX_ATTEMPTS = 5;

    /** Kilitlenme suresi (ms): 15 dakika. */
    private static final long LOCK_DURATION_MS = 15 * 60 * 1000L;

    private record AttemptRecord(int count, Instant lockedUntil) {
    }

    private final ConcurrentHashMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    /**
     * Verilen anahtar (kullanici adi) kilitli mi?
     *
     * @return true ise islem reddedilmeli
     */
    public boolean isBlocked(String key) {
        AttemptRecord record = attempts.get(normalise(key));
        if (record == null) {
            return false;
        }
        if (record.lockedUntil() != null && Instant.now().isBefore(record.lockedUntil())) {
            return true;
        }
        // Kilit suresi dolmussa temizle
        if (record.lockedUntil() != null) {
            attempts.remove(normalise(key));
        }
        return false;
    }

    /** Basarisiz giris kaydeder; gerekirse kilidi uygular. */
    public void recordFailure(String key) {
        String k = normalise(key);
        AttemptRecord current = attempts.getOrDefault(k, new AttemptRecord(0, null));
        int newCount = current.count() + 1;

        if (newCount >= MAX_ATTEMPTS) {
            Instant lockedUntil = Instant.now().plusMillis(LOCK_DURATION_MS);
            attempts.put(k, new AttemptRecord(newCount, lockedUntil));
            log.warn("Giris kilitlendi ({}): {} basarisiz deneme", k, newCount);
        } else {
            attempts.put(k, new AttemptRecord(newCount, null));
        }
    }

    /** Basarili giriste sayaci sifirla. */
    public void recordSuccess(String key) {
        attempts.remove(normalise(key));
    }

    /** Kalan kilit suresi (saniye); kilitli degilse 0. */
    public long remainingLockSeconds(String key) {
        AttemptRecord record = attempts.get(normalise(key));
        if (record == null || record.lockedUntil() == null) {
            return 0;
        }
        long remaining = record.lockedUntil().getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, remaining);
    }

    /** Her 30 dakikada bir suresi dolmus kayitlari temizler (bellek sızıntısı onleme). */
    @Scheduled(fixedDelay = 30 * 60 * 1000)
    public void cleanup() {
        Instant now = Instant.now();
        int before = attempts.size();
        attempts.entrySet().removeIf(e ->
                e.getValue().lockedUntil() != null && now.isAfter(e.getValue().lockedUntil())
        );
        int removed = before - attempts.size();
        if (removed > 0) {
            log.debug("LoginAttemptService temizlendi: {} kayit silindi", removed);
        }
    }

    private String normalise(String key) {
        return key == null ? "" : key.trim().toLowerCase();
    }
}
