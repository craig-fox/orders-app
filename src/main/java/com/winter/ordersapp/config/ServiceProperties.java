package com.winter.ordersapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "services")
public class ServiceProperties {
    private final Payment payment = new Payment();
    private final Inventory inventory = new Inventory(); // 1. Add the field

    public Payment getPayment() {
        return payment;
    }

    public Inventory getInventory() { // 2. Add the getter
        return inventory;
    }

    public static class Payment {
        private String baseUrl;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }

    // 3. Add the nested Inventory class
    public static class Inventory {
        private String baseUrl;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }
}

