package com.pdfFileReader.backend;

import com.pdfFileReader.domain.entity.AuthToken;
import com.pdfFileReader.domain.entity.User;
import com.pdfFileReader.domain.service.impl.AuthService;
import com.pdfFileReader.exception.AuthException;
import com.pdfFileReader.repository.AuthTokenRepository;
import com.pdfFileReader.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AuthServiceIntegrationTest {

    private static final String PREFIX = "auth-test-" + System.currentTimeMillis();

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthTokenRepository tokenRepository;

    @AfterEach
    void cleanup() {
        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getUsername().startsWith(PREFIX))
                .toList();
        for (User user : users) {
            tokenRepository.findAll().stream()
                    .filter(t -> t.getUserId().equals(user.getId()))
                    .forEach(t -> tokenRepository.deleteById(t.getId()));
            userRepository.deleteById(user.getId());
        }
    }

    @Test
    void registerLoginAndValidateRoundTrip() {
        AuthToken registered = authService.register(PREFIX, "guclu-parola");

        assertTrue(authService.validate(registered.getToken()).isPresent(), "kayit token'i gecerli olmali");

        AuthToken logged = authService.login(PREFIX, "guclu-parola");
        Optional<User> user = authService.validate(logged.getToken());

        assertTrue(user.isPresent(), "giris token'i gecerli olmali");
        assertEquals(PREFIX, user.get().getUsername());
    }

    @Test
    void wrongPasswordRejected() {
        authService.register(PREFIX, "dogru-parola");

        assertThrows(AuthException.class, () -> authService.login(PREFIX, "yanlis-parola"));
    }

    @Test
    void duplicateUsernameRejected() {
        authService.register(PREFIX, "parola-1");

        assertThrows(AuthException.class, () -> authService.register(PREFIX, "parola-2"));
    }

    @Test
    void shortPasswordRejected() {
        assertThrows(AuthException.class, () -> authService.register(PREFIX, "123"));
    }

    @Test
    void unknownTokenRejected() {
        assertTrue(authService.validate("olmayan-token").isEmpty());
    }
}
