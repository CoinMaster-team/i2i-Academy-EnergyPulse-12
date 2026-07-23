package com.coinmaster.energypulse.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(BusinessRuleException.class)
        public ResponseEntity<ApiErrorResponse> handleBusinessRuleException(
                        BusinessRuleException exception,
                        HttpServletRequest request) {
                ApiErrorResponse response = createResponse(
                                HttpStatus.BAD_REQUEST,
                                exception.getCode(),
                                exception.getMessage(),
                                request.getRequestURI(),
                                Map.of());

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(response);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiErrorResponse> handleValidationException(
                        MethodArgumentNotValidException exception,
                        HttpServletRequest request) {
                Map<String, String> fieldErrors = new LinkedHashMap<>();

                exception.getBindingResult()
                                .getFieldErrors()
                                .forEach(fieldError -> fieldErrors.putIfAbsent(
                                                fieldError.getField(),
                                                fieldError.getDefaultMessage()));

                ApiErrorResponse response = createResponse(
                                HttpStatus.BAD_REQUEST,
                                "VALIDATION_ERROR",
                                "Request validation failed.",
                                request.getRequestURI(),
                                fieldErrors);

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(response);
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleResourceNotFoundException(
                        ResourceNotFoundException exception,
                        HttpServletRequest request) {
                ApiErrorResponse response = createResponse(
                                HttpStatus.NOT_FOUND,
                                exception.getCode(),
                                exception.getMessage(),
                                request.getRequestURI(),
                                Map.of());

                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(response);
        }

        @ExceptionHandler(NoResourceFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleNoResourceFoundException(
                        NoResourceFoundException exception,
                        HttpServletRequest request) {
                ApiErrorResponse response = createResponse(
                                HttpStatus.NOT_FOUND,
                                "RESOURCE_NOT_FOUND",
                                "The requested resource was not found.",
                                request.getRequestURI(),
                                Map.of());

                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(response);
        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
                        DataIntegrityViolationException exception,
                        HttpServletRequest request) {
                LOGGER.warn("Database integrity violation.", exception);

                ApiErrorResponse response = createResponse(
                                HttpStatus.CONFLICT,
                                "DATA_INTEGRITY_VIOLATION",
                                "The request conflicts with existing database data.",
                                request.getRequestURI(),
                                Map.of());

                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(response);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
                        Exception exception,
                        HttpServletRequest request) {
                LOGGER.error("Unexpected application error.", exception);

                ApiErrorResponse response = createResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "INTERNAL_SERVER_ERROR",
                                "An unexpected error occurred.",
                                request.getRequestURI(),
                                Map.of());

                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(response);
        }

        private ApiErrorResponse createResponse(
                        HttpStatus status,
                        String code,
                        String message,
                        String path,
                        Map<String, String> fieldErrors) {
                return new ApiErrorResponse(
                                OffsetDateTime.now(ZoneOffset.UTC),
                                status.value(),
                                status.getReasonPhrase(),
                                code,
                                message,
                                path,
                                fieldErrors);
        }
}