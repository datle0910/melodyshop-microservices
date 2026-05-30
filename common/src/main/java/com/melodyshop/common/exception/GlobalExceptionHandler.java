package com.melodyshop.common.exception;

import com.melodyshop.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for all microservices.
 * Each service can extend this class to add service-specific handlers.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), "NOT_FOUND"));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), "BAD_REQUEST"));
    }

    @ExceptionHandler(ProductInOrderException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductInOrder(ProductInOrderException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), "PRODUCT_IN_ORDER"));
    }

    @ExceptionHandler(FeignClientException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeignClientException(FeignClientException ex) {
        int status = ex.getStatus();
        HttpStatus httpStatus;
        String errorCode;
        if (status == 400) {
            httpStatus = HttpStatus.BAD_REQUEST;
            errorCode = "BAD_REQUEST";
        } else if (status == 401 || status == 403) {
            httpStatus = HttpStatus.FORBIDDEN;
            errorCode = "FORBIDDEN";
        } else if (status == 404) {
            httpStatus = HttpStatus.NOT_FOUND;
            errorCode = "NOT_FOUND";
        } else if (status == 409) {
            httpStatus = HttpStatus.CONFLICT;
            errorCode = "CONFLICT";
        } else {
            httpStatus = HttpStatus.BAD_GATEWAY;
            errorCode = "BAD_GATEWAY";
        }
        return ResponseEntity.status(httpStatus)
                .body(ApiResponse.error(ex.getMessage(), errorCode));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Dữ liệu không hợp lệ")
                        .error("VALIDATION_ERROR")
                        .data(errors)
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Lỗi hệ thống: " + ex.getMessage(), "INTERNAL_ERROR"));
    }
}
