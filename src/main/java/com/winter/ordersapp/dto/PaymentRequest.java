package com.winter.ordersapp.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record PaymentRequest( 
    @NotBlank(message = "orderId must not be blank")
    String orderId,

    @Positive(message = "amount must be greater than 0")
    BigDecimal amount) {}
