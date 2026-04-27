package com.winter.ordersapp.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.winter.ordersapp.client.InventoryClient;
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
    private final PaymentGateway paymentService;
    private final OrderRepository repository;
    private final InventoryClient inventoryClient;

    public OrderService(OrderRepository repository, InventoryClient inventoryClient, PaymentGateway paymentService) {
        this.repository = repository;
        this.inventoryClient = inventoryClient;
        this.paymentService = paymentService;

    }

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


    private OrderResponse map(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getCustomerId(),
            order.getStatus().name(),
            order.getTotalAmount(),
            order.getCreatedAt()
        );
}

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {

        Order order = createPendingOrder(request); // no annotation here

        try {
            paymentService.processPayment(order);
            inventoryClient.reserve(order);
            order.setStatus(OrderStatus.CONFIRMED);

        } catch (PaymentException _) {
            order.setStatus(OrderStatus.PAYMENT_FAILED);

        } catch (InventoryException _) {
            order.setStatus(OrderStatus.INVENTORY_FAILED);
            
        } catch (Exception _) {
            order.setStatus(OrderStatus.PROCESSING_ERROR);
        }

        return map(order);
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
