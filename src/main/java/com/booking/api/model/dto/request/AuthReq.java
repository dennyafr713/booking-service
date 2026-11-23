package com.booking.api.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthReq {

    @NotBlank(message = "email tidak boleh kosong")
    private String email;

    @NotBlank(message = "password tidak boleh kosong")
    private String password;
}
