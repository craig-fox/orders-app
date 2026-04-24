package com.winter.ordersapp.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
    UUID id,

    String customerId,

    String status,

    BigDecimal totalAmount,

    Instant createdAt
) {

}
