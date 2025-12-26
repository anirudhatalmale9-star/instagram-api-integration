package com.instagram.api.exception;

public class InstagramApiException extends RuntimeException {

    private final String errorCode;
    private final int statusCode;

    public InstagramApiException(String message) {
        super(message);
        this.errorCode = "INSTAGRAM_API_ERROR";
        this.statusCode = 500;
    }

    public InstagramApiException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = 500;
    }

    public InstagramApiException(String message, String errorCode, int statusCode) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }

    public InstagramApiException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "INSTAGRAM_API_ERROR";
        this.statusCode = 500;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
