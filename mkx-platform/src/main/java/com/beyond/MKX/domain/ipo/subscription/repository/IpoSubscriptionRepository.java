package com.beyond.MKX.domain.ipo.subscription.repository;

import com.beyond.MKX.domain.ipo.subscription.entity.IpoSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface IpoSubscriptionRepository extends JpaRepository<IpoSubscription, UUID> {
    boolean existsByIpoOffering_IdAndAccountId(UUID ipoOfferingId, UUID accountId);
}
