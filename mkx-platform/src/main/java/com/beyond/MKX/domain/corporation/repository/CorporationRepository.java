package com.beyond.MKX.domain.corporation.repository;

import com.beyond.MKX.domain.corporation.entity.Corporation;
import com.beyond.MKX.domain.corporation.entity.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CorporationRepository extends JpaRepository<Corporation, UUID> {
    // 사업자등록번호 중복 여부 검증
    boolean existsByRegNo(String regNo);

    // 상태별 기업 목록 조회
    List<Corporation> findAllByStatus(Status status);

    @Query(
            value = """
                select c
                from Corporation c
                where c.deletedAt is null
                  and (:status is null or c.status = :status)
                  and (
                       :q is null
                    or lower(c.nameKo) like lower(concat('%', :q, '%'))
                    or lower(c.nameEng) like lower(concat('%', :q, '%'))
                    or c.regNo like concat('%', :q, '%')
                  )
                """,
            countQuery = """
                select count(c)
                from Corporation c
                where c.deletedAt is null
                  and (:status is null or c.status = :status)
                  and (
                       :q is null
                    or lower(c.nameKo) like lower(concat('%', :q, '%'))
                    or lower(c.nameEng) like lower(concat('%', :q, '%'))
                    or c.regNo like concat('%', :q, '%')
                  )
                """
    )
    Page<Corporation> search(
            @Param("status") Status status,
            @Param("q") String q,
            Pageable pageable
    );
}
