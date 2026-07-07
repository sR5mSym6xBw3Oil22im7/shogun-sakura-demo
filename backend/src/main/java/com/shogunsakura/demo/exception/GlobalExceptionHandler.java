package com.shogunsakura.demo.exception;

import com.shogunsakura.demo.dto.ErrorResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> fieldErrors = new LinkedHashMap<>();
    ex.getBindingResult().getFieldErrors().forEach(fieldError ->
        fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage()));

    return ResponseEntity.badRequest().body(new ErrorResponse("入力内容を確認してください。", fieldErrors));
  }

  @ExceptionHandler({
      HttpMessageNotReadableException.class,
      MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {
    return ResponseEntity.badRequest().body(new ErrorResponse("入力内容を確認してください。", Map.of()));
  }

  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<ErrorResponse> handleDataAccess(DataAccessException ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponse("現在、注文を受け付けできません。時間をおいて再度お試しください。", Map.of()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponse("現在、注文を受け付けできません。時間をおいて再度お試しください。", Map.of()));
  }
}
