package com.winter.ordersapp.dto;

import java.math.BigDecimal;

public record PaymentRequest( String orderId, BigDecimal amount) {

}
