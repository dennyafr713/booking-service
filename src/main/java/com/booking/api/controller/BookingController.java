package com.booking.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.booking.api.model.dto.request.BookReq;
import com.booking.api.model.dto.response.BookingRes;
import com.booking.api.model.dto.response.CancelBookRes;
import com.booking.api.model.entity.User;
import com.booking.api.service.BookingService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Bookings", description = "Booking dan pembatalan booking ruangan")
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingRes create(@AuthenticationPrincipal User principal,
            @Valid @RequestBody BookReq request) {
        return bookingService.doBook(principal, request);
    }

    @PatchMapping("/{id}/cancel")
    public CancelBookRes cancel(@AuthenticationPrincipal User principal,
            @PathVariable Long id) {
        return bookingService.doCancel(principal, id);
    }
}
