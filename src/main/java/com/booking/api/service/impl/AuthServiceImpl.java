package com.booking.api.service.impl;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import com.booking.api.exception.ApiException;
import com.booking.api.exception.ErrorCode;
import com.booking.api.model.dto.request.AuthReq;
import com.booking.api.model.dto.response.AuthRes;
import com.booking.api.model.entity.User;
import com.booking.api.security.JwtService;
import com.booking.api.service.AuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Override
    public AuthRes login(AuthReq request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.getEmail().trim(), request.getPassword()));
        } catch (AuthenticationException e) {
            log.debug("Login gagal: {}", e.getMessage());
            throw ApiException.unauthorized(ErrorCode.INVALID_CREDENTIALS, "Email atau password salah");
        }

        User user = (User) authentication.getPrincipal();
        return AuthRes.builder().token(jwtService.generateToken(user)).build();
    }
}
