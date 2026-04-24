package com.winter.ordersapp.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.winter.ordersapp.domain.Order;

public interface OrderRepository extends JpaRepository<Order, UUID> {

}
