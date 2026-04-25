package com.winter.ordersapp.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "orders")
public class Order {
    @Id
    private UUID id;

    private String customerId;

    @Enumerated(EnumType.STRING)

    private OrderStatus status;

    private BigDecimal totalAmount;

    private Instant createdAt;

    public Order() {}

    public Order(UUID id, String customerId, OrderStatus status,

                 BigDecimal totalAmount, Instant createdAt) {

        this.id = id;

        this.customerId = customerId;

        this.status = status;

        this.totalAmount = totalAmount;

        this.createdAt = createdAt;

    }

   

    
}
