package com.booking.api.service.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.booking.api.exception.ApiException;
import com.booking.api.exception.ErrorCode;
import com.booking.api.model.dto.request.CreateRoomReq;
import com.booking.api.model.dto.response.AvailableRoomRes;
import com.booking.api.model.dto.response.RoomRes;
import com.booking.api.model.entity.Room;
import com.booking.api.repository.RoomRepository;
import com.booking.api.service.RoomService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;

    @Override
    @Transactional
    public RoomRes doCreate(CreateRoomReq request) {
        Room room = roomRepository.save(Room.builder()
                .name(request.getName().trim())
                .capacity(request.getCapacity())
                .build());

        return RoomRes.from(room);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailableRoomRes> doFindAvailable(Instant start, Instant end) {
        if (!end.isAfter(start)) {
            throw ApiException.badRequest(ErrorCode.INVALID_TIME,
                    "waktu berakhir harus lebih besar dari waktu mulai");
        }

        return roomRepository.findAvailable(toUtc(start), toUtc(end)).stream()
                .map(AvailableRoomRes::from)
                .toList();
    }

    private static LocalDateTime toUtc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
