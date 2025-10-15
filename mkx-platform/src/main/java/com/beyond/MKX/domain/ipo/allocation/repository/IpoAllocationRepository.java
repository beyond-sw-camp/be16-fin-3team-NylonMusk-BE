package com.beyond.MKX.domain.ipo.allocation.repository;

import com.beyond.MKX.domain.ipo.allocation.entity.IpoAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IpoAllocationRepository extends JpaRepository<IpoAllocation, UUID> {
    boolean existsByIpoSubscription_IdAndRoundNo(UUID subscriptionId, Integer roundNo);

    Optional<IpoAllocation> findTopByIpoSubscription_IdOrderByRoundNoDesc(UUID subscriptionId);
}
