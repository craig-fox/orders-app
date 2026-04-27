package com.winter.ordersapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.winter.ordersapp.client.InventoryClient;
import com.winter.ordersapp.domain.Order;
import com.winter.ordersapp.domain.OrderStatus;
import com.winter.ordersapp.dto.OrderRequest;
import com.winter.ordersapp.dto.OrderResponse;
import com.winter.ordersapp.exception.InventoryException;
import com.winter.ordersapp.exception.PaymentException;
import com.winter.ordersapp.repository.OrderRepository;


@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock PaymentGateway paymentService;

    @Mock InventoryClient inventoryClient;

    @Mock OrderRepository repository;

    @InjectMocks OrderService orderService;

    @Test
    void shouldReturnOrderWhenFound() {
        UUID orderId = UUID.randomUUID();

        Order order = new Order(
            orderId,
            "cust-123",
            OrderStatus.CONFIRMED,
            BigDecimal.valueOf(50),
            Instant.now()
        );

        Mockito.when(repository.findById(orderId))
            .thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrder(orderId);

        assertEquals(orderId, response.id());
        assertEquals("cust-123", response.customerId());
        assertEquals("CONFIRMED", response.status());
        assertEquals(BigDecimal.valueOf(50), response.totalAmount());
    }

    @Test
    void shouldReturnPaymentFailedWhenPaymentExceptionOccurs() {

        OrderRequest request = new OrderRequest("cust-123", BigDecimal.valueOf(50));
        when(repository.save(any(Order.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        doThrow(new PaymentException("fail"))
        .when(paymentService)
        .processPayment(any());    


        OrderResponse response = orderService.createOrder(request);

        assertEquals("PAYMENT_FAILED", response.status());

    }

    @Test
    void shouldReturnProcessingErrorForUnknownException() {

        OrderRequest request = new OrderRequest("cust-123", BigDecimal.valueOf(50));
        when(repository.save(any(Order.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("boom"))
            .when(inventoryClient)
            .reserve(any());    


        OrderResponse response = orderService.createOrder(request);

        assertEquals("PROCESSING_ERROR", response.status());

    }

    @Test
    void shouldMarkInventoryFailure() {

        when(repository.save(any(Order.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        doThrow(new InventoryException("fail"))
            .when(inventoryClient)
            .reserve(any());    


        OrderRequest request = new OrderRequest("cust-123", BigDecimal.valueOf(50));
        OrderResponse response = orderService.createOrder(request);

        assertEquals("INVENTORY_FAILED", response.status());

    }
}
