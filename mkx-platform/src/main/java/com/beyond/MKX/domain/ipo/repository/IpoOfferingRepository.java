package com.beyond.MKX.domain.ipo.repository;

import com.beyond.MKX.domain.ipo.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.entity.IpoOfferingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from IpoOffering o where o.id = :id")
    Optional<IpoOffering> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select count(o) from IpoOffering o " +
            "where o.ipo.id = :ipoId and o.ipoOfferingStatus = 'OPEN'")
    long countOpenForIpoForUpdate(@Param("ipoId") UUID ipoId);
}
