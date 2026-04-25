package com.winter.ordersapp.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.winter.ordersapp.client.InventoryClient;
import com.winter.ordersapp.client.PaymentClient;
import com.winter.ordersapp.domain.Order;
import com.winter.ordersapp.domain.OrderStatus;
import com.winter.ordersapp.dto.OrderRequest;
import com.winter.ordersapp.dto.OrderResponse;
import com.winter.ordersapp.exception.InventoryException;
import com.winter.ordersapp.exception.OrderNotFoundException;
import com.winter.ordersapp.exception.PaymentException;
import com.winter.ordersapp.repository.OrderRepository;

import lombok.extern.slf4j.Slf4j;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@Service
public class OrderService {
    private final OrderRepository repository;
    private final PaymentClient paymentClient;
    private final InventoryClient inventoryClient;

    public OrderService(OrderRepository repository, PaymentClient paymentClient, InventoryClient inventoryClient) {

        this.repository = repository;
        this.paymentClient = paymentClient;
        this.inventoryClient = inventoryClient;

    }


    public OrderResponse createOrder(OrderRequest request) {

        Order order = new Order(
            UUID.randomUUID(),
            request.customerId(),
            OrderStatus.PENDING,
            request.totalAmount(),
            Instant.now()
        );

        repository.save(order);

        try {
            paymentClient.processPayment(order);
            inventoryClient.reserve(order);
            order.setStatus(OrderStatus.CONFIRMED);
        } catch (PaymentException e) {
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            log.warn("payment_failed orderId={}", order.getId(), e);
        } catch (InventoryException e) {
            order.setStatus(OrderStatus.INVENTORY_FAILED);
        }

        log.info("payment_success orderId={}", order.getId());

        return new OrderResponse(
            order.getId(),
            order.getCustomerId(),
            order.getStatus().name(),
            order.getTotalAmount(),
            order.getCreatedAt()
        );
    }

    public OrderResponse getOrder(UUID id) {
        Order order = repository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        log.info("retrieved_order",
            kv("orderId", order.getId()),
            kv("customerId", order.getCustomerId()));

        return new OrderResponse(
            order.getId(),
            order.getCustomerId(),
            order.getStatus().name(),
            order.getTotalAmount(),
            order.getCreatedAt()
        );
    }

}
