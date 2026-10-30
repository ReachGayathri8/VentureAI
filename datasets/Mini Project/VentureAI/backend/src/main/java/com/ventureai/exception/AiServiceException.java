package com.ventureai.exception;

/** Thrown when the ML service or Gemini call fails; callers catch this and
 * fall back gracefully rather than letting the request 500. */
public class AiServiceException extends RuntimeException {
    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
    public AiServiceException(String message) {
        super(message);
    }
}
