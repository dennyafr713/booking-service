package com.booking.api.exception;

public final class ErrorCode {

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String INVALID_REQUEST_BODY = "INVALID_REQUEST_BODY";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String INVALID_TIME = "INVALID_TIME";
    public static final String ROOM_NOT_FOUND = "ROOM_NOT_FOUND";
    public static final String ROOM_INACTIVE = "ROOM_INACTIVE";
    public static final String ROOM_CONFLICT = "ROOM_CONFLICT";
    public static final String BOOKING_NOT_FOUND = "BOOKING_NOT_FOUND";
    public static final String BOOKING_ALREADY_CANCELLED = "BOOKING_ALREADY_CANCELLED";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";

    private ErrorCode() {
    }
}
