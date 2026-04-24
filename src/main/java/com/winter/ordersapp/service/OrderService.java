package com.winter.ordersapp.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.winter.ordersapp.domain.Order;
import com.winter.ordersapp.domain.OrderStatus;
import com.winter.ordersapp.dto.CreateOrderRequest;
import com.winter.ordersapp.dto.OrderResponse;
import com.winter.ordersapp.exception.OrderNotFoundException;
import com.winter.ordersapp.repository.OrderRepository;

import lombok.extern.slf4j.Slf4j;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
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
        log.info("order_created",
            kv("orderId", order.getId()),
            kv("customerId", order.getCustomerId()));

        return new OrderResponse(
                saved.getId(),
                saved.getCustomerId(),
                saved.getStatus().name(),
                saved.getTotalAmount(),
                saved.getCreatedAt()
        );

    }

    public OrderResponse getOrder(UUID id) {
        Order order = repository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getCreatedAt()
        );
    }

}
