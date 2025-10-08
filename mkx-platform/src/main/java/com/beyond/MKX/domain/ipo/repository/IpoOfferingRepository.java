package com.beyond.MKX.domain.ipo.repository;

import com.beyond.MKX.domain.ipo.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.entity.IpoOfferingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IpoOfferingRepository extends JpaRepository<IpoOffering, UUID> {
    boolean existsByIpo_IdAndRoundNo(UUID ipoId, Integer roundNo);

    boolean existsByIpo_Id(UUID ipoId);

    boolean existsByIpo_IdAndIpoOfferingStatusIn(UUID ipoId, Collection<IpoOfferingStatus> statuses);

    Optional<IpoOffering> findTopByIpo_IdOrderByRoundNoDesc(UUID ipoId);
}
