package com.pdfFileReader.controller;

import com.pdfFileReader.domain.entity.AuthToken;
import com.pdfFileReader.domain.service.impl.AuthService;
import com.pdfFileReader.dto.AuthRequest;
import com.pdfFileReader.dto.AuthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody AuthRequest request) {
        AuthToken token = authService.register(request.username(), request.password());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(token.getToken(), token.getUserId().toString()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        AuthToken token = authService.login(request.username(), request.password());

        return ResponseEntity.ok(new AuthResponse(token.getToken(), token.getUserId().toString()));
    }
}
