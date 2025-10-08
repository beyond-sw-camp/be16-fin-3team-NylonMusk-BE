package com.beyond.MKX.domain.ipo.repository;

import com.beyond.MKX.domain.ipo.entity.IpoOffering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IpoOfferingRepository extends JpaRepository<IpoOffering, UUID> {
    boolean existsByIpo_IdAndRoundNo(UUID ipoId, Integer roundNo);
}
