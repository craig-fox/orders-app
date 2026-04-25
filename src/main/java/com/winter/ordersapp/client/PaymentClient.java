package com.winter.ordersapp.client;

import com.winter.ordersapp.domain.Order;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/payments") // Base path for the Payments app
public interface PaymentClient {

    @PostExchange // This tells the Proxy: "Turn this into a POST request"
    void processPayment(@RequestBody Order order);
}