package com.booking.api.model.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookReq {

    @NotNull(message = "ruangan id tidak boleh kosong")
    private Long roomId;

    @NotBlank(message = "judul tidak boleh kosong")
    @Size(max = 150, message = "judul maksimal 150 karakter")
    private String title;

    @NotNull(message = "jam mulai tidak boleh kosong")
    private LocalDateTime startTime;

    @NotNull(message = "jam selesai tidak boleh kosong")
    private LocalDateTime endTime;
}
