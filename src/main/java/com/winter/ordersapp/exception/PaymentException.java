package com.winter.ordersapp.exception;

import org.springframework.web.client.RestClientException;

public class PaymentException extends RuntimeException {
    public PaymentException(String message) {
        super(message);
    }

    public PaymentException(String string, RestClientException e) {
        //TODO Auto-generated constructor stub
    }

}
