package com.beyond.MKX.domain.disclosure.repository;

import com.beyond.MKX.domain.disclosure.entity.Disclosure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.beyond.MKX.domain.disclosure.entity.DisclosureType;
import com.beyond.MKX.domain.disclosure.entity.DisclosureStatus;

@Repository
public interface DisclosureRepository extends JpaRepository<Disclosure, UUID> {
    @Query("select d from Disclosure d " +
           "where d.stockId = :stockId " +
           "  and lower(trim(d.title)) = lower(trim(:title)) " +
           "  and d.createdAt > :threshold " +
           "order by d.createdAt desc")
    Optional<Disclosure> findRecentDuplicate(
            @Param("stockId") UUID stockId,
            @Param("title") String title,
            @Param("threshold") LocalDateTime threshold
    );

    @Query("""
            select d
            from Disclosure d
            where d.status = com.beyond.MKX.domain.disclosure.entity.DisclosureStatus.APPROVED
              and d.publishedAt is not null
              and (:type is null or d.disclosureType = :type)
              and (:stockId is null or d.stockId = :stockId)
              and (:title is null or lower(d.title) like lower(concat('%', :title, '%')))
            order by d.publishedAt desc
            """)
    Page<Disclosure> searchApproved(
            @Param("type") DisclosureType type,
            @Param("stockId") UUID stockId,
            @Param("title") String title,
            Pageable pageable
    );

    @Query("""
            select d
            from Disclosure d
            where (:status is null or d.status = :status)
              and (:type is null or d.disclosureType = :type)
              and (:stockId is null or d.stockId = :stockId)
              and (:title is null or lower(d.title) like lower(concat('%', :title, '%')))
              and (:from is null or d.createdAt >= :from)
              and (:toExclusive is null or d.createdAt < :toExclusive)
            """)
    Page<Disclosure> searchAdmin(
            @Param("status") DisclosureStatus status,
            @Param("type") DisclosureType type,
            @Param("stockId") UUID stockId,
            @Param("title") String title,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive,
            Pageable pageable
    );

    @Query("""
            select d
            from Disclosure d
            where d.stockId in (
                select s.id from com.beyond.MKX.domain.stock.entity.Stock s where s.corporationId = :corpId
            )
              and (:status is null or d.status = :status)
              and (:type is null or d.disclosureType = :type)
              and (:title is null or lower(d.title) like lower(concat('%', :title, '%')))
              and (:from is null or d.createdAt >= :from)
              and (:toExclusive is null or d.createdAt < :toExclusive)
            """)
    Page<Disclosure> searchByCorporation(
            @Param("corpId") UUID corporationId,
            @Param("status") DisclosureStatus status,
            @Param("type") DisclosureType type,
            @Param("title") String title,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive,
            Pageable pageable
    );

    @Query("select coalesce(max(d.revisionNo), 0) from Disclosure d where (d.originId = :rootId or d.id = :rootId)")
    Integer findMaxRevisionInGroup(@Param("rootId") UUID rootId);
}
