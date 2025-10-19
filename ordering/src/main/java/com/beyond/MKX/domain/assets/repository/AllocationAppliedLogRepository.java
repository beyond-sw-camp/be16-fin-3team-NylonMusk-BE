package com.beyond.MKX.domain.assets.repository;

import com.beyond.MKX.domain.assets.entity.AllocationAppliedLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AllocationAppliedLogRepository extends JpaRepository<AllocationAppliedLog, UUID> {
    boolean existsByAllocationEventId(UUID allocationEventId);
}
