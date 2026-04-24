package com.winter.cameron.dto;

import java.math.BigDecimal;

public record CreateOrderRequest(

        String customerId,

        BigDecimal totalAmount

) {}

