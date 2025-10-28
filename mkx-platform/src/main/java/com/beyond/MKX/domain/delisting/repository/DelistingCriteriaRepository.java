package com.beyond.MKX.domain.delisting.repository;

import com.beyond.MKX.domain.delisting.entity.DelistingCriteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DelistingCriteriaRepository extends JpaRepository<DelistingCriteria, UUID> {

    /**
     * 기준 코드로 조회 (활성화된 것만)
     */
    @Query("SELECT dc FROM DelistingCriteria dc WHERE dc.criteriaCode = :code AND dc.isActive = true")
    Optional<DelistingCriteria> findByCriteriaCodeAndActive(@Param("code") String criteriaCode);

    /**
     * 기준 코드 중복 검사
     */
    boolean existsByCriteriaCode(String criteriaCode);

    /**
     * 활성화된 모든 기준 조회
     */
    @Query("SELECT dc FROM DelistingCriteria dc WHERE dc.isActive = true ORDER BY dc.criteriaType, dc.criteriaCode")
    List<DelistingCriteria> findAllActive();

    /**
     * 활성화된 모든 기준 조회 (자동화용)
     */
    List<DelistingCriteria> findByIsActiveTrue();

    /**
     * 기준 유형별 활성화된 기준 조회
     */
    @Query("SELECT dc FROM DelistingCriteria dc WHERE dc.criteriaType = :type AND dc.isActive = true ORDER BY dc.criteriaCode")
    List<DelistingCriteria> findByCriteriaTypeAndActive(@Param("type") com.beyond.MKX.domain.delisting.entity.CriteriaType criteriaType);

    /**
     * 단건 조회 (삭제 포함) - Command 경로에서 복구/재삭제 등을 위해 필요
     */
    @Query(value = "SELECT * FROM delisting_criteria WHERE id = :id", nativeQuery = true)
    Optional<DelistingCriteria> findOneIncludingDeleted(@Param("id") UUID id);

    /**
     * 활성(미삭제)만 조회 - 수정 시 보수적으로 사용
     */
    @Query(value = "SELECT * FROM delisting_criteria WHERE id = :id AND deleted_at IS NULL", nativeQuery = true)
    Optional<DelistingCriteria> findActiveById(@Param("id") UUID id);
}
