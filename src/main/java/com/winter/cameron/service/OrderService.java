package com.winter.cameron.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.winter.cameron.domain.Order;
import com.winter.cameron.domain.OrderStatus;
import com.winter.cameron.dto.CreateOrderRequest;
import com.winter.cameron.dto.OrderResponse;
import com.winter.cameron.repository.OrderRepository;

@Service
public class OrderService {
    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {

        this.repository = repository;

    }

    public OrderResponse createOrder(CreateOrderRequest request) {

        Order order = new Order(

                UUID.randomUUID(),

                request.customerId(),

                OrderStatus.CREATED,

                request.totalAmount(),

                Instant.now()

        );

        Order saved = repository.save(order);

        return new OrderResponse(

                saved.getId(),

                saved.getCustomerId(),

                saved.getStatus().name(),

                saved.getTotalAmount(),

                saved.getCreatedAt()

        );

    }

}
