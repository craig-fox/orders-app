package com.winter.ordersapp.client;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import com.winter.ordersapp.domain.Order;

@HttpExchange("/inventory")
public interface InventoryClient {
    @PostExchange("/reserve")
    void reserve(@RequestBody Order order);
}
