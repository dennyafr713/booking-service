package com.booking.api.model.dto.response;

import com.booking.api.model.entity.Room;

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
public class AvailableRoomRes {

    private Long id;
    private String name;
    private Integer capacity;

    public static AvailableRoomRes from(Room room) {
        return AvailableRoomRes.builder()
                .id(room.getId())
                .name(room.getName())
                .capacity(room.getCapacity())
                .build();
    }
}
