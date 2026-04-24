package com.winter.ordersapp.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.winter.ordersapp.exception.OrderNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

     @ResponseStatus(HttpStatus.BAD_REQUEST)

    @ExceptionHandler(MethodArgumentNotValidException.class)

    public Map<String, String> handleValidation(MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors()

                .forEach(error ->

                        errors.put(error.getField(), error.getDefaultMessage())

                );

        return errors;

    }



    @ResponseStatus(HttpStatus.NOT_FOUND)

    @ExceptionHandler(OrderNotFoundException.class)

    public Map<String, Object> handleNotFound(OrderNotFoundException ex) {

        return Map.of(
                "error", "ORDER_NOT_FOUND",
                "message", ex.getMessage(),
                "timestamp", Instant.now()
        );

    }

}
