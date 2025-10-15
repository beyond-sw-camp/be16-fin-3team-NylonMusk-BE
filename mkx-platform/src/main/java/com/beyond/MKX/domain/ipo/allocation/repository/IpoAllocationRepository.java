package com.beyond.MKX.domain.ipo.allocation.repository;

import com.beyond.MKX.domain.ipo.allocation.entity.IpoAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.*;

@Repository
public interface IpoAllocationRepository extends JpaRepository<IpoAllocation, UUID> {
    boolean existsByIpoSubscription_IdAndRoundNo(UUID subscriptionId, Integer roundNo);

    Optional<IpoAllocation> findTopByIpoSubscription_IdOrderByRoundNoDesc(UUID subscriptionId);

    @Query("select a from IpoAllocation a where a.ipoSubscription.ipoOffering.id = :offeringId")
    List<IpoAllocation> findAllByOfferingId(UUID offeringId);
}
