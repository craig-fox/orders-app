package com.winter.ordersapp.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import com.winter.ordersapp.client.PaymentClient;
import com.winter.ordersapp.domain.Order;
import com.winter.ordersapp.exception.PaymentException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Service
public class PaymentGateway {

    private final PaymentClient paymentClient;

    public PaymentGateway(PaymentClient paymentClient) {
        this.paymentClient = paymentClient;
    }

    @Retry(name = "paymentRetry", fallbackMethod = "paymentFallback")
    @CircuitBreaker(name = "paymentCircuit")
    public void processPayment(Order order) {
        paymentClient.processPayment(order);
    }

    private void paymentFallback(Order order, RestClientException e) {
        throw new PaymentException("Payment failed after retries", e );
    }
}