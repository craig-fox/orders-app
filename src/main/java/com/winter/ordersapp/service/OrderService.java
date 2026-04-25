package com.winter.ordersapp.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import com.winter.ordersapp.client.InventoryClient;
import com.winter.ordersapp.client.PaymentClient;
import com.winter.ordersapp.domain.Order;
import com.winter.ordersapp.domain.OrderStatus;
import com.winter.ordersapp.dto.OrderRequest;
import com.winter.ordersapp.dto.OrderResponse;
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

    @Retryable(retryFor = PaymentException.class, maxAttempts = 3, backoff = @Backoff(delay = 500))
    public OrderResponse createOrder(OrderRequest request) {
        // ... (Order creation and saving logic) ...
        Order order = new Order(
            UUID.randomUUID(),
            request.customerId(),
            OrderStatus.PENDING,
            request.totalAmount(),
            Instant.now()
        );

        // No try-catch here! Let the exception fly so @Retryable can see it.
        paymentClient.processPayment(order);
        inventoryClient.reserve(order);
        
        order.setStatus(OrderStatus.CONFIRMED);
        repository.save(order); // Save the success

        return new OrderResponse(
            order.getId(),
            order.getCustomerId(),
            order.getStatus().name(),
            order.getTotalAmount(),
            order.getCreatedAt()
        );
    }

    @Recover
    public OrderResponse handlePaymentFailure(PaymentException e, OrderRequest request) {
        log.warn("Payment retries exhausted for customer: {}", request.customerId());
        
        // This is where you set the failure status after 3 failed attempts
        return new OrderResponse(
            null, 
            request.customerId(), 
            "PAYMENT_FAILED", 
            request.totalAmount(), 
            Instant.now()
        );
    }

    @Recover
    public OrderResponse handleTimeout(ResourceAccessException e, OrderRequest request) {
        log.error("Payment timed out after retries: {}", e.getMessage());
        
        // Return the response that your test is looking for
        return new OrderResponse(
            null, 
            request.customerId(), 
            "PAYMENT_FAILED", 
            request.totalAmount(), 
            Instant.now()
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
