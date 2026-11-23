package com.booking.api.service;

import java.time.Instant;
import java.util.List;

import com.booking.api.model.dto.request.CreateRoomReq;
import com.booking.api.model.dto.response.AvailableRoomRes;
import com.booking.api.model.dto.response.RoomRes;

public interface RoomService {

    RoomRes doCreate(CreateRoomReq request);

    List<AvailableRoomRes> doFindAvailable(Instant start, Instant end);
}