package com.beyond.MKX.domain.ipo.offering.repository;

import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOfferingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
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

    boolean existsByIpo_IdAndRoundNoLessThanAndIpoOfferingStatusNotIn(
            UUID ipoId, Integer roundNo, Collection<IpoOfferingStatus> statuses
    );

    @Query("select max(o.roundNo) from IpoOffering o where o.ipo.id = :ipoId")
    Integer findMaxRoundNo(UUID ipoId);

    @Query("""
            select o.issuedQuantity
            from IpoOffering o
            where o.ipo.id = :ipoId
              and o.ipoOfferingStatus = :status
              and o.roundNo = (
                  select max(o2.roundNo)
                  from IpoOffering o2
                  where o2.ipo.id = :ipoId
                    and o2.ipoOfferingStatus = :status
              )
            """)
    Optional<Long> findLatestSettledIssuedQuantity(UUID ipoId,
                                                   IpoOfferingStatus status);


    Page<IpoOffering> findByIpo_Id(UUID ipoId, Pageable pageable);

    Page<IpoOffering> findByIpo_IdAndIpoOfferingStatusIn(
            UUID ipoId, Collection<IpoOfferingStatus> statuses, Pageable pageable);

    Page<IpoOffering> findByIpoOfferingStatusIn(
            Collection<IpoOfferingStatus> statuses, Pageable pageable);

    @Query("""
               select o from IpoOffering o
               where o.ipoOfferingStatus = 'OPEN'
                 and :now between o.subscriptionStart and o.subscriptionEnd
            """)
    Page<IpoOffering> findCurrentlySubscribable(@Param("now") LocalDateTime now, Pageable pageable);

    // 수요예측 가능한 공모 목록 조회
    List<IpoOffering> findAllByIpoOfferingStatus(IpoOfferingStatus status);

    List<IpoOffering> findAllByIpoOfferingStatusAndBookBuildingEndBefore(
            IpoOfferingStatus ipoOfferingStatus,
            LocalDateTime bookBuildingEnd
    );


}
