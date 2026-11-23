package com.booking.api.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.booking.api.model.dto.request.CreateRoomReq;
import com.booking.api.model.dto.response.AvailableRoomRes;
import com.booking.api.model.dto.response.RoomRes;
import com.booking.api.service.RoomService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomRes create(@Valid @RequestBody CreateRoomReq request) {
        return roomService.doCreate(request);
    }

    @GetMapping("/available")
    public List<AvailableRoomRes> findAvailable(
            @RequestParam("start_time") Instant start,
            @RequestParam("end_time") Instant end) {
        return roomService.doFindAvailable(start, end);
    }
}
