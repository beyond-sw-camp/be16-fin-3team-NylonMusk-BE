package com.beyond.MKX.domain.ipo.IpoAllocationOutbox.repository;

import com.beyond.MKX.domain.ipo.IpoAllocationOutbox.entity.IpoAllocationOutbox;
import com.beyond.MKX.domain.ipo.IpoAllocationOutbox.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface IpoAllocationOutboxRepository extends JpaRepository<IpoAllocationOutbox, UUID> {
    List<IpoAllocationOutbox> findAllByIpoIdAndStatus(UUID ipoId, OutboxStatus status);
    List<IpoAllocationOutbox> findAllByStatus(OutboxStatus status); // 재시도용
}
