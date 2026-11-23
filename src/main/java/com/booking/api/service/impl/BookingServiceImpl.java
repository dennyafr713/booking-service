package com.booking.api.service.impl;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.booking.api.enums.BookingStatus;
import com.booking.api.exception.ApiException;
import com.booking.api.exception.ErrorCode;
import com.booking.api.model.dto.request.BookReq;
import com.booking.api.model.dto.response.BookingRes;
import com.booking.api.model.dto.response.CancelBookRes;
import com.booking.api.model.entity.Booking;
import com.booking.api.model.entity.Room;
import com.booking.api.model.entity.User;
import com.booking.api.repository.BookingRepository;
import com.booking.api.repository.RoomRepository;
import com.booking.api.repository.UserRepository;
import com.booking.api.service.BookingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public BookingRes doBook(User principal, BookReq request) {
        LocalDateTime start = request.getStartTime();
        LocalDateTime end = request.getEndTime();
        if (!end.isAfter(start)) {
            throw ApiException.badRequest(ErrorCode.INVALID_TIME,
                    "waktu berakhir harus lebih besar dari waktu mulai");
        }

        if (start.isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            throw ApiException.badRequest(ErrorCode.INVALID_TIME,
                    "waktu mulai tidak boleh di masa lalu");
        }

        Room room = roomRepository.findByIdForUpdate(request.getRoomId())
                .orElseThrow(() -> ApiException.notFound(ErrorCode.ROOM_NOT_FOUND,
                        "Ruangan " + request.getRoomId() + " tidak ditemukan"));
        if (!room.isActive()) {
            throw ApiException.badRequest(ErrorCode.ROOM_INACTIVE,
                    "Ruangan " + room.getId() + " tidak aktif");
        }

        if (bookingRepository.existsOverlapping(room.getId(), BookingStatus.CONFIRMED, start, end)) {
            throw roomConflict();
        }

        User user = userRepository.getReferenceById(principal.getId());
        Booking booking = Booking.builder()
                .room(room)
                .user(user)
                .title(request.getTitle().trim())
                .startTime(start)
                .endTime(end)
                .build();
        try {
            booking = bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException e) {
            log.warn("Booking bentrok terdeteksi oleh constraint DB: {}", e.getMostSpecificCause().getMessage());
            throw roomConflict();
        }

        return BookingRes.from(booking);
    }

    @Override
    @Transactional
    public CancelBookRes doCancel(User principal, Long bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.BOOKING_NOT_FOUND,
                        "Booking " + bookingId + " tidak ditemukan"));

        if (!principal.isAdmin() && !booking.getUser().getId().equals(principal.getId())) {
            throw ApiException.forbidden("Anda hanya dapat membatalkan booking milik sendiri");
        }

        if (booking.isCancelled()) {
            throw ApiException.conflict(ErrorCode.BOOKING_ALREADY_CANCELLED,
                    "Booking sudah dibatalkan sebelumnya");
        }

        booking.cancel();
        return CancelBookRes.from(booking);
    }

    private static ApiException roomConflict() {
        return ApiException.conflict(ErrorCode.ROOM_CONFLICT,
                "Ruangan sudah dibooking pada rentang waktu tersebut");
    }
}
