package com.winter.ordersapp.integration;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jayway.jsonpath.JsonPath;
import com.winter.ordersapp.client.InventoryClient;
import com.winter.ordersapp.client.PaymentClient;
import com.winter.ordersapp.exception.PaymentException;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class OrderIntegrationTest {

    @MockitoBean // <--- Add this
    private PaymentClient paymentClient;

    @MockitoBean // <--- Add this
    private InventoryClient inventoryClient;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateOrder() throws Exception {
        Mockito.doNothing().when(paymentClient).processPayment(Mockito.any());
        Mockito.doNothing().when(inventoryClient).reserve(Mockito.any());
        String request = """
        {
           "customerId": "cust-123",
           "totalAmount": 49.99
       }
        """;

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void shouldGetOrder() throws Exception {
        // First create an order
        Mockito.doNothing().when(paymentClient).processPayment(Mockito.any());
        Mockito.doNothing().when(inventoryClient).reserve(Mockito.any());
        String request = """
        {
           "customerId": "cust-123",
           "totalAmount": 49.99
       }
        """;

        String response = mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        // Extract the order ID from the response
        String orderId = JsonPath.read(response, "$.id");

        // Now retrieve the order
        mockMvc.perform(get("/orders/" + orderId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(orderId))
            .andExpect(jsonPath("$.customerId").value("cust-123"))
            .andExpect(jsonPath("$.totalAmount").value(49.99));
    }

    @Test
    void shouldRejectOrder() throws Exception {
        String request = """
         {
                "customerId": "   ",
                "totalAmount": -10
         }
        """;
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isBadRequest());
    } 
    
    @Test
    void shouldHandlePaymentFailure() throws Exception {
        Mockito.doThrow(new PaymentException("Payment failed"))
            .when(paymentClient).processPayment(Mockito.any());

        String request = """
        {
           "customerId": "cust-123",
           "totalAmount": 49.99
       }
        """;

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PAYMENT_FAILED"));
    }

    @Test
    void shouldHandleInventoryFailure() throws Exception {
        Mockito.doNothing().when(paymentClient).processPayment(Mockito.any());
        Mockito.doThrow(new RuntimeException("Inventory failed"))
            .when(inventoryClient).reserve(Mockito.any());

        String request = """
        {
           "customerId": "cust-123",
           "totalAmount": 49.99
       }
        """;

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("INVENTORY_FAILED"));
    }

    @Test
    void shouldHandlePaymentRetries() throws Exception {
        // 1. Stub the client to always throw the exception.
        // Spring Retry will manage the number of attempts (3).
        Mockito.doThrow(new PaymentException("Payment failed"))
            .when(paymentClient).processPayment(Mockito.any());

        String request = """
        {
        "customerId": "cust-123",
        "totalAmount": 49.99
        }
        """;

        // 2. Perform the request. 
        // Because we use @Recover in the Service, the request 
        // will eventually return 201/200 with the "PAYMENT_FAILED" status.
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PAYMENT_FAILED"));

        // 3. Verify that the client was called exactly 3 times.
        // This proves the retry logic actually executed.
        Mockito.verify(paymentClient, Mockito.times(3)).processPayment(Mockito.any());
    }

    @Test
    void shouldHandleTimeoutByMarkingOrderFailed() throws Exception {
        // 1. Explicitly throw the exception Spring throws during a timeout
        Mockito.doThrow(new org.springframework.web.client.ResourceAccessException("Read timed out"))
            .when(paymentClient).processPayment(Mockito.any());

        String request = """
        {
            "customerId": "cust-123",
            "totalAmount": 49.99
        }
        """;

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isCreated()) 
            .andExpect(jsonPath("$.status").value("PAYMENT_FAILED"));
    }

    

          
}