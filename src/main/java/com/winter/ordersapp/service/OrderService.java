package com.winter.ordersapp.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.winter.ordersapp.client.InventoryClient;
import com.winter.ordersapp.client.PaymentClient;
import com.winter.ordersapp.domain.Order;
import com.winter.ordersapp.domain.OrderStatus;
import com.winter.ordersapp.dto.OrderRequest;
import com.winter.ordersapp.dto.OrderResponse;
import com.winter.ordersapp.exception.OrderNotFoundException;
import com.winter.ordersapp.exception.PaymentException;
import com.winter.ordersapp.repository.OrderRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@Service
public class OrderService {
    private final OrderRepository repository;
    private final PaymentClient paymentClient;
    private final InventoryClient inventoryClient;

    public OrderService(OrderRepository repository, PaymentClient paymentClient, InventoryClient inventoryClient) {
        log.warn("DEBUG: PaymentClient class is: " + paymentClient.getClass().getName());
        this.repository = repository;
        this.paymentClient = paymentClient;
        this.inventoryClient = inventoryClient;

    }

    @Transactional
    public Order createPendingOrder(OrderRequest request) {
        Order order = new Order(
            UUID.randomUUID(),
            request.customerId(),
            OrderStatus.PENDING,
            request.totalAmount(),
            Instant.now()
        );
        return repository.save(order);
    }

    @CircuitBreaker(name = "paymentCircuit")
    @Retry(name = "paymentRetry", fallbackMethod = "paymentFallback")
    public void processPayment(Order order) {
        paymentClient.processPayment(order);
    }

    @Transactional
    public void markOrderFailed(UUID orderId) {
        Order order = repository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.PAYMENT_FAILED);
    }

    @Transactional
    public void markOrderStatus(UUID orderId, OrderStatus status) {
        Order order = repository.findById(orderId).orElseThrow();
        order.setStatus(status);
    }

    private OrderResponse map(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getCustomerId(),
            order.getStatus().name(),
            order.getTotalAmount(),
            order.getCreatedAt()
        );
}

    
    public OrderResponse createOrder(OrderRequest request) {

        Order order = createPendingOrder(request);

        try {
            processPayment(order);

            inventoryClient.reserve(order);
            order.setStatus(OrderStatus.CONFIRMED);

        } catch (PaymentException e) {
            markOrderStatus(order.getId(), OrderStatus.PAYMENT_FAILED);
            order.setStatus(OrderStatus.PAYMENT_FAILED);

        } catch (Exception e) {
            markOrderStatus(order.getId(), OrderStatus.PROCESSING_ERROR);
            order.setStatus(OrderStatus.PROCESSING_ERROR);
        }

        return map(order);
    }

    // private OrderResponse paymentFallback(OrderRequest request, Throwable ex) {

    //     log.error("Payment failed after retries for customer {}", request.customerId(), ex);

    //     if (ex instanceof PaymentException) {
    //         return createFailureResponse(request, "PAYMENT_FAILED");
    //     }

    //     return createFailureResponse(request, "PROCESSING_ERROR");
    // }

    private OrderResponse createFailureResponse(OrderRequest request, String status) {
    return new OrderResponse(
        UUID.randomUUID(),
        request.customerId(),
        status,
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
