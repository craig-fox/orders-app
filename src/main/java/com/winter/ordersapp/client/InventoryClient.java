package com.winter.ordersapp.client;

import com.winter.ordersapp.domain.Order;

public interface InventoryClient {
    void reserve(Order order);
}
