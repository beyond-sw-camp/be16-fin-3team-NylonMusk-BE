package com.beyond.MKX.domain.ipo.subscription.repository;

import com.beyond.MKX.domain.ipo.subscription.entity.IpoSubscription;
import com.beyond.MKX.domain.ipo.subscription.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface IpoSubscriptionRepository extends JpaRepository<IpoSubscription, UUID> {
    boolean existsByIpoOffering_IdAndAccountId(UUID ipoOfferingId, UUID accountId);

    @Query("""
   select coalesce(sum(s.appliedQuantity), 0)
   from IpoSubscription s
   where s.ipoOffering.id = :offeringId
     and s.status = :status
""")
    long sumAppliedQuantityByOffering(@Param("offeringId") UUID offeringId,
                                      @Param("cancelled") SubscriptionStatus cancelled);
}
