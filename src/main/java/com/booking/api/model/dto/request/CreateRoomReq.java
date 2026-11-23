package com.booking.api.model.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRoomReq {

    @NotBlank(message = "nama tidak boleh kosong")
    @Size(max = 100, message = "nama maksimal 100 karakter")
    private String name;

    @NotNull(message = "kapasitas tidak boleh kosong")
    @Min(value = 1, message = "kapasitas minimal 1")
    private Integer capacity;
}
