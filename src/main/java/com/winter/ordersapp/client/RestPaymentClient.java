package com.winter.ordersapp.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.winter.ordersapp.config.ServiceProperties;
import com.winter.ordersapp.domain.Order;
import com.winter.ordersapp.dto.PaymentRequest;
import com.winter.ordersapp.dto.PaymentResponse;
import com.winter.ordersapp.exception.PaymentException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RestPaymentClient implements PaymentClient {
    private final RestClient restClient;
    private final String baseUrl;

    public RestPaymentClient(RestClient restClient,
                             ServiceProperties serviceProperties) {
        this.restClient = restClient;
        this.baseUrl = serviceProperties.getPayment().getBaseUrl();
    }


    @Retryable( 
        retryFor  = { RestClientException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 500)
   )
    @Override
    public void processPayment(Order order) {

        PaymentRequest request = new PaymentRequest(
            order.getId().toString(),
            order.getTotalAmount()
        );
        log.info("payment_request orderId={} amount={}",
            order.getId(), order.getTotalAmount());

        try {

            PaymentResponse response = restClient.post()
                .uri(baseUrl + "/api/v1/payments")
                .body(request)
                .retrieve()
                .body(PaymentResponse.class);

            if (response == null || !"SUCCESS".equals(response.status())) {
                throw new PaymentException("Payment failed");
            }

        } catch (RestClientException e) {
            throw new PaymentException("Payment service call failed", e);
        }

    }

}
