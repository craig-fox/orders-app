package com.winter.cameron.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.winter.cameron.domain.Order;

public interface OrderRepository extends JpaRepository<Order, UUID> {

}
