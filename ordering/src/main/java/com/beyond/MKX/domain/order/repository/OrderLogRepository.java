package com.beyond.MKX.domain.order.repository;

import com.beyond.MKX.domain.order.entity.OrderLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderLogRepository extends JpaRepository<OrderLog, UUID> {
}
