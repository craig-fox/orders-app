package com.winter.ordersapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.winter.ordersapp.client.InventoryClient;
import com.winter.ordersapp.client.PaymentClient;
import com.winter.ordersapp.dto.OrderRequest;
import com.winter.ordersapp.dto.OrderResponse;
import com.winter.ordersapp.exception.PaymentException;
import com.winter.ordersapp.repository.OrderRepository;


@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
 @Mock PaymentClient paymentClient;

    @Mock InventoryClient inventoryClient;

    @Mock OrderRepository repository;

    @InjectMocks OrderService orderService;

    @Test
    void shouldReturnPaymentFailedWhenPaymentExceptionOccurs() {

        OrderRequest request = new OrderRequest("cust-123", BigDecimal.valueOf(50));

        Mockito.doThrow(new PaymentException("fail"))

            .when(paymentClient).processPayment(Mockito.any());

        OrderResponse response = orderService.createOrder(request);

        assertEquals("PAYMENT_FAILED", response.status());

    }

    @Test
    void shouldReturnProcessingErrorForUnknownException() {

        OrderRequest request = new OrderRequest("cust-123", BigDecimal.valueOf(50));

        Mockito.doThrow(new RuntimeException("boom"))

            .when(paymentClient).processPayment(Mockito.any());

        OrderResponse response = orderService.createOrder(request);

        assertEquals("PROCESSING_ERROR", response.status());

    }

    @Test
    void shouldMarkInventoryFailure() {

        Mockito.doNothing().when(paymentClient).processPayment(Mockito.any());

        Mockito.doThrow(new RuntimeException("inventory down"))

            .when(inventoryClient).reserve(Mockito.any());

        OrderRequest request = new OrderRequest("cust-123", BigDecimal.valueOf(50));

        OrderResponse response = orderService.createOrder(request);

        assertEquals("INVENTORY_FAILED", response.status());

    }
}
