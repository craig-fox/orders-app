package com.winter.ordersapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry(order = Ordered.LOWEST_PRECEDENCE)
public class RetryConfig {

}
