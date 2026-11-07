package com.inno.orderservice.exception;

import feign.FeignException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(OrderNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(ItemNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ItemNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(UserServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleUserServiceUnavailable(UserServiceUnavailableException exception) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeign(FeignException exception) {
        return switch (exception.status()) {
            case 401 -> build(HttpStatus.UNAUTHORIZED, "Not authorized in user service");
            case 403 -> build(HttpStatus.FORBIDDEN, "Access denied");
            case 404 -> build(HttpStatus.NOT_FOUND, "User not found");
            default -> build(HttpStatus.SERVICE_UNAVAILABLE, "User service is unavailable");
        };
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        return build(HttpStatus.CONFLICT, "Database constraint violation");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return build(HttpStatus.BAD_REQUEST, "Parameter '" + exception.getName() + "' has invalid type");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
        return build(HttpStatus.BAD_REQUEST, "Malformed JSON request or invalid data format");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        StringBuilder message = new StringBuilder();

        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            message.append(error.getField()).append(": ").append(error.getDefaultMessage()).append("; ");
        }

        return build(HttpStatus.BAD_REQUEST, message.toString());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestParameter(
            MissingServletRequestParameterException exception) {

        return build(HttpStatus.BAD_REQUEST, "Required parameter '" + exception.getParameterName() + "' is missing");
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message
        );

        return ResponseEntity.status(status).body(error);
    }
}
