package com.winter.ordersapp.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.winter.ordersapp.dto.PaymentRequest;
import com.winter.ordersapp.dto.PaymentResponse;
import com.winter.ordersapp.exception.PaymentException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PaymentService {
    public PaymentResponse process(PaymentRequest request) {

        log.info("payment_requested orderId={} amount={}", request.orderId(), request.amount());

            // simulate business logic

            if (request.amount().compareTo(BigDecimal.valueOf(100)) > 0) {
                return new PaymentResponse("FAILED");
            }

            if (Math.random() < 0.3) {
                throw new PaymentException("Random payment failure");
            }

            var result = new PaymentResponse("SUCCESS");
            log.info("payment_result orderId={} status={}", request.orderId(), result.status());
            return result;

        }
}
