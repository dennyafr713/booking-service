package com.booking.api.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking.api.model.dto.request.AuthReq;
import com.booking.api.model.dto.response.AuthRes;
import com.booking.api.service.AuthService;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Auth", description = "Autentikasi dan penerbitan JWT")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @SecurityRequirements // endpoint publik, tidak butuh Bearer token
    @PostMapping("/login")
    public AuthRes login(@Valid @RequestBody AuthReq request) {
        return authService.doLogin(request);
    }
}
