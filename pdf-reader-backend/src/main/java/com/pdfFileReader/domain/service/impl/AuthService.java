package com.pdfFileReader.domain.service.impl;

import com.pdfFileReader.domain.entity.AuthToken;
import com.pdfFileReader.domain.entity.User;
import com.pdfFileReader.exception.AuthException;
import com.pdfFileReader.repository.AuthTokenRepository;
import com.pdfFileReader.repository.UserRepository;
import com.pdfFileReader.util.PasswordHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Kayit/giris/cikis ve token dogrulama. Token'lar DB'de opaque bearer
 * token olarak tutulur; sunucu tarafinda iptal edilebilir.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final AuthTokenRepository tokenRepository;
    private final int tokenTtlDays;
    private final SecureRandom random = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            AuthTokenRepository tokenRepository,
            @Value("${app.auth.token-ttl-days:30}") int tokenTtlDays
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.tokenTtlDays = tokenTtlDays;
    }

    @Transactional
    public AuthToken register(String username, String password) {
        String name = normalize(username);
        validatePassword(password);

        if (userRepository.existsByUsername(name)) {
            throw new AuthException("Bu kullanici adi zaten kayitli.");
        }

        User user = new User();
        user.setUsername(name);
        user.setPasswordHash(PasswordHasher.hash(password));
        userRepository.save(user);

        log.info("Yeni kullanici kaydedildi: {}", name);
        return issueToken(user);
    }

    @Transactional
    public AuthToken login(String username, String password) {
        String name = normalize(username);

        User user = userRepository.findByUsername(name)
                .orElseThrow(() -> new AuthException("Kullanici adi veya parola hatali."));

        if (!PasswordHasher.verify(password, user.getPasswordHash())) {
            throw new AuthException("Kullanici adi veya parola hatali.");
        }

        log.info("Kullanici giris yapti: {}", name);
        return issueToken(user);
    }

    public Optional<User> validate(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        AuthToken stored = tokenRepository.findByToken(token).orElse(null);
        if (stored == null) {
            return Optional.empty();
        }

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(stored);
            return Optional.empty();
        }

        return userRepository.findById(stored.getUserId());
    }

    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            tokenRepository.deleteByToken(token);
        }
    }

    private AuthToken issueToken(User user) {
        byte[] raw = new byte[48];
        random.nextBytes(raw);

        AuthToken authToken = new AuthToken();
        authToken.setToken(Base64.getUrlEncoder().withoutPadding().encodeToString(raw));
        authToken.setUserId(user.getId());
        authToken.setExpiresAt(LocalDateTime.now().plusDays(tokenTtlDays));
        return tokenRepository.save(authToken);
    }

    private String normalize(String username) {
        if (username == null || username.isBlank()) {
            throw new AuthException("Kullanici adi bos olamaz.");
        }
        String trimmed = username.trim();
        if (trimmed.length() < 3 || trimmed.length() > 64) {
            throw new AuthException("Kullanici adi 3-64 karakter olmali.");
        }
        return trimmed;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new AuthException("Parola en az 6 karakter olmali.");
        }
    }
}
