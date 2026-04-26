package com.winter.ordersapp.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

@SpringBootTest(properties = {

    "services.payment.base-url=http://localhost:${wiremock.server.port}"

})

@AutoConfigureMockMvc
class OrderPaymentIntegrationTest {
  @RegisterExtension

    static WireMockExtension wireMock =

        WireMockExtension.newInstance()

            .options(wireMockConfig().dynamicPort())

            .build();

    @Autowired MockMvc mockMvc;

    @Test

    void shouldRetryPaymentAndFallbackToPaymentFailed() throws Exception {

        wireMock.stubFor(post(urlEqualTo("/api/v1/payments"))

            .willReturn(aResponse().withStatus(500)));

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

        wireMock.verify(3,

            postRequestedFor(urlEqualTo("/api/v1/payments")));

    }

    @Test

    void shouldHandleTimeout() throws Exception {

        wireMock.stubFor(post(urlEqualTo("/api/v1/payments"))

            .willReturn(aResponse().withFixedDelay(5000)));

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
