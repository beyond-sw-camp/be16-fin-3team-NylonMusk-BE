package com.beyond.MKX.domain.securities_firm.repository;

import com.beyond.MKX.domain.securities_firm.entity.SecuritiesFirm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SecuritiesFirmRepository extends JpaRepository<SecuritiesFirm, UUID> {
    // 사업자등록번호 중복 여부 검증
    boolean existsByRegNo(String regNo);

    // 금융투자업 인가번호 중복 여부 검증
    boolean existsByFinancialInvestmentLicenseNo(String financialInvestmentLicenseNo);

    // 상태별 증권사 목록 조회
    List<SecuritiesFirm> findAllByStatus(SecuritiesFirm.Status status);

    //  페이징 처리된 상태별 증권사 목록 조회
    Page<SecuritiesFirm> findAllByStatus(SecuritiesFirm.Status status, Pageable pageable);

    //  검색 기능 포함 페이징 (국문명 또는 영문명)
    @Query("SELECT s FROM SecuritiesFirm s WHERE s.status = :status AND s.deletedAt IS NULL " +
            "AND (:search IS NULL OR LOWER(s.nameKo) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(s.nameEng) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<SecuritiesFirm> searchByStatusAndName(
            @Param("status") SecuritiesFirm.Status status,
            @Param("search") String search,
            Pageable pageable
    );

    @Query(
            value = """
                select s
                from SecuritiesFirm s
                where s.deletedAt is null
                  and (:status is null or s.status = :status)
                  and (
                       :q is null
                    or lower(s.nameKo) like lower(concat('%', :q, '%'))
                    or lower(s.nameEng) like lower(concat('%', :q, '%'))
                    or s.regNo like concat('%', :q, '%')
                  )
                """,
            countQuery = """
                select count(s)
                from SecuritiesFirm s
                where s.deletedAt is null
                  and (:status is null or s.status = :status)
                  and (
                       :q is null
                    or lower(s.nameKo) like lower(concat('%', :q, '%'))
                    or lower(s.nameEng) like lower(concat('%', :q, '%'))
                    or s.regNo like concat('%', :q, '%')
                  )
                """
    )
    Page<SecuritiesFirm> searchAdmin(
            @Param("status") SecuritiesFirm.Status status,
            @Param("q") String q,
            Pageable pageable
    );
}
