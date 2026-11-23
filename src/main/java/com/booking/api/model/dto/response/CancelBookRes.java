package com.booking.api.model.dto.response;

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
public class CancelBookRes {

    private Long id;
    private BookingStatus status;

    public static CancelBookRes from(Booking b) {
        return new CancelBookRes(b.getId(), b.getStatus());
    }
}
