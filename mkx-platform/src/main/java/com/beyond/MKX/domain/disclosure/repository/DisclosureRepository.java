package com.beyond.MKX.domain.disclosure.repository;

import com.beyond.MKX.domain.disclosure.entity.Disclosure;
import com.beyond.MKX.domain.disclosure.entity.DisclosureStatus;
import com.beyond.MKX.domain.disclosure.entity.DisclosureType;
import com.beyond.MKX.domain.disclosure.entity.DisclosureRelationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DisclosureRepository extends JpaRepository<Disclosure, UUID> {
    // 등록 가드: DisclosureService.register()에서 최근 제목 중복 여부 확인
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

    // 공개 조회: DisclosureQueryService.listApproved() (/public/disclosures)
    @Query("""
            select d
            from Disclosure d
            where d.status = :approved
              and d.publishedAt is not null
              and (:type is null or d.disclosureType = :type)
              and (:stockId is null or d.stockId = :stockId)
              and (:title is null or lower(d.title) like lower(concat('%', :title, '%')))
              and (:displayNo is null or d.displayNo = :displayNo)
            order by d.publishedAt desc
            """)
    Page<Disclosure> searchApproved(
            @Param("approved") DisclosureStatus approved,
            @Param("type") DisclosureType type,
            @Param("stockId") UUID stockId,
            @Param("title") String title,
            @Param("displayNo") String displayNo,
            Pageable pageable
    );

    // 관리자 목록 조회: DisclosureAdminQueryService.search() (/admin/disclosures)
    @Query("""
            select d
            from Disclosure d
            where (:status is null or d.status = :status)
              and (:type is null or d.disclosureType = :type)
              and (:stockId is null or d.stockId = :stockId)
              and (:title is null or lower(d.title) like lower(concat('%', :title, '%')))
              and (:displayNo is null or d.displayNo = :displayNo)
              and (:from is null or d.createdAt >= :from)
              and (:toExclusive is null or d.createdAt < :toExclusive)
            """)
    Page<Disclosure> searchAdmin(
            @Param("status") DisclosureStatus status,
            @Param("type") DisclosureType type,
            @Param("stockId") UUID stockId,
            @Param("title") String title,
            @Param("displayNo") String displayNo,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive,
            Pageable pageable
    );

    // 기업(소속) 목록 조회: DisclosureCorpQueryService.listMine() (/disclosures/my)
    @Query("""
            select d
            from Disclosure d
            where d.stockId in (
                select s.id from Stock s where s.corporationId = :corpId
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

    // 리비전 회차 조회: DisclosureService.revise() (정정 등록 시 next 회차 계산)
    @Query("select coalesce(max(d.revisionNo), 0) from Disclosure d where (d.originId = :rootId or d.id = :rootId)")
    Integer findMaxRevisionInGroup(@Param("rootId") UUID rootId);

    // 그룹 공시번호 조회: DisclosureAdminService.approve() (정정 승인 시 displayNo 상속)
    @Query("select distinct d.displayNo from Disclosure d where (d.originId = :rootId or d.id = :rootId) and d.displayNo is not null")
    List<String> findGroupDisplayNo(@Param("rootId") UUID rootId);

    // 최신본 토글: DisclosureAdminService.approve() (isLatest 갱신)
    @Modifying(clearAutomatically = true)
    @Query("update Disclosure d set d.isLatest = false where (d.originId = :rootId or d.id = :rootId) and d.isLatest = true")
    int clearLatestByGroup(@Param("rootId") UUID rootId);

    // 리비전 이력 조회: DisclosureAdminQueryService.listRevisionsByDisplayNo() (/admin/disclosures/{displayNo}/revisions)
    @Query("""
            select d
            from Disclosure d
            where d.displayNo = :displayNo
            order by d.revisionNo desc, d.createdAt desc
            """)
    List<Disclosure> findRevisionsByDisplayNo(@Param("displayNo") String displayNo);

    // 원본 공시 조회: DisclosureAdminQueryService.getRelatedTreeByBaseNo() (관련 공시 트리에서 원본 포함)
    @Query("""
            select d
            from Disclosure d
            where d.displayNo = :displayNo
              and d.originId is null
            """)
    List<Disclosure> findByDisplayNoAndOriginIdIsNull(@Param("displayNo") String displayNo);
    
    // displayNo로 공시 조회: DisclosureAdminQueryService.getRelatedTreeByBaseNo() (역추적용)
    @Query("""
            select d
            from Disclosure d
            where d.displayNo = :displayNo
            """)
    List<Disclosure> findByDisplayNo(@Param("displayNo") String displayNo);

    // 추가공시 조회: previousId가 체인 내 ID에 속하는 ADDITIONAL 목록
    @Query("""
            select d
            from Disclosure d
            where d.relationType = :relationType
              and d.previousId in :previousIds
            order by d.createdAt asc
            """)
    List<Disclosure> findAdditionalsByPreviousIds(@Param("relationType") DisclosureRelationType relationType,
                                                  @Param("previousIds") List<UUID> previousIds);

    /** 공개 조회 배치: 다중 티커 스냅샷 기준 */
    @Query("""
            select d
            from Disclosure d
            where d.status = :approved
              and d.publishedAt is not null
              and (:type is null or d.disclosureType = :type)
              and d.tickerSnapshot in :tickers
            order by d.publishedAt desc
            """)
    Page<Disclosure> searchApprovedByTickers(
            @Param("approved") DisclosureStatus approved,
            @Param("type") DisclosureType type,
            @Param("tickers") List<String> tickers,
            Pageable pageable
    );
}
