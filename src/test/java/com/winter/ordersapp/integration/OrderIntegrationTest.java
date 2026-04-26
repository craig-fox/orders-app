package com.winter.ordersapp.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.jayway.jsonpath.JsonPath;
import com.winter.ordersapp.client.InventoryClient;
import com.winter.ordersapp.security.JwtFilter;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.client.WireMock.*;


@SpringBootTest(properties = {
    "services.payment.base-url=http://localhost:${wiremock.server.port}"
})
@Testcontainers
@AutoConfigureMockMvc(addFilters = false)
class OrderIntegrationTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    @MockitoBean // <--- Add this
    private InventoryClient inventoryClient;

    @MockitoBean
    private JwtFilter jwtFilter;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Autowired
    private RestClient.Builder restClientBuilder;


    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        wireMock.resetAll();

        wireMock.stubFor(post("/api/v1/payments")
            .willReturn(aResponse()
                .withStatus(500)));
    }

    @Test
    void shouldCreateOrder() throws Exception {
       // nothing needed if PaymentClient is real + WireMock handles it
        Mockito.doNothing().when(inventoryClient).reserve(Mockito.any());
        String request = """
        {
           "customerId": "cust-123",
           "totalAmount": 49.99
       }
        """;
       
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andDo(print())
                .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void shouldGetOrder() throws Exception {
        // First create an order
       // nothing needed if PaymentClient is real + WireMock handles it
        Mockito.doNothing().when(inventoryClient).reserve(Mockito.any());
        String request = """
        {
           "customerId": "cust-123",
           "totalAmount": 49.99
       }
        """;

        String response = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/orders")
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
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isBadRequest());
    } 
    
    @Test
    void shouldHandlePaymentRetries() throws Exception {

        wireMock.stubFor(post("/api/v1/payments")
            .willReturn(aResponse()
                .withStatus(500)));

        String request = """
        {
            "customerId": "cust-123",
            "totalAmount": 49.99
        }
        """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PAYMENT_FAILED"));

        wireMock.verify(3,
            postRequestedFor(urlEqualTo("/api/v1/payments"))
        );
    }

    @Test
    void shouldHandleInventoryFailure() throws Exception {
        // nothing needed if PaymentClient is real + WireMock handles it
        Mockito.doThrow(new RuntimeException("Inventory failed"))
            .when(inventoryClient).reserve(Mockito.any());

        String request = """
        {
           "customerId": "cust-123",
           "totalAmount": 49.99
       }
        """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("INVENTORY_FAILED"));
    }

    @Test
    void shouldHandleTimeoutByMarkingOrderFailed() throws Exception {

        wireMock.stubFor(post(urlEqualTo("/api/v1/payments"))
            .willReturn(aResponse()
                .withStatus(500)));

        String request = """
        {
            "customerId": "cust-123",
            "totalAmount": 49.99
        }
        """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PAYMENT_FAILED"));
    }

    

          
}