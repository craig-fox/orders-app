package com.winter.cameron.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateOrderRequest(

    @NotBlank(message = "customerId must not be blank")    
    String customerId,

        
    @Positive(message = "totalAmount must be greater than 0")
    BigDecimal totalAmount

) {}

