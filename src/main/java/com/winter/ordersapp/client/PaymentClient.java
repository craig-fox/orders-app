package com.winter.ordersapp.client;

import com.winter.ordersapp.domain.Order;

public interface PaymentClient {
    void processPayment(Order order);
}
