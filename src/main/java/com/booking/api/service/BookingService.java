package com.booking.api.service;

import com.booking.api.model.dto.request.BookReq;
import com.booking.api.model.dto.response.BookingRes;
import com.booking.api.model.dto.response.CancelBookRes;
import com.booking.api.model.entity.User;

public interface BookingService {

    BookingRes doBook(User principal, BookReq request);

    CancelBookRes doCancel(User principal, Long bookingId);
}
