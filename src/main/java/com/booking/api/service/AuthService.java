package com.booking.api.service;

import com.booking.api.model.dto.request.AuthReq;
import com.booking.api.model.dto.response.AuthRes;

public interface AuthService {

    AuthRes doLogin(AuthReq request);
}
