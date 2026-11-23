package com.booking.api.model.dto.response;

import com.booking.api.model.entity.Room;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomRes {

    private Long id;
    private String name;
    private Integer capacity;

    @JsonProperty("is_active")
    private boolean active;

    public static RoomRes from(Room room) {
        return RoomRes.builder()
                .id(room.getId())
                .name(room.getName())
                .capacity(room.getCapacity())
                .active(room.isActive())
                .build();
    }
}
