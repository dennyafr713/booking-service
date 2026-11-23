package com.booking.api.model.dto.response;

import java.time.LocalDateTime;

import com.booking.api.enums.BookingStatus;
import com.booking.api.model.entity.Booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingRes {

    private Long id;
    private Long roomId;
    private Long userId;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BookingStatus status;

    public static BookingRes from(Booking b) {
        return BookingRes.builder()
                .id(b.getId())
                .roomId(b.getRoom().getId())
                .userId(b.getUser().getId())
                .title(b.getTitle())
                .startTime(b.getStartTime())
                .endTime(b.getEndTime())
                .status(b.getStatus())
                .build();
    }
}
