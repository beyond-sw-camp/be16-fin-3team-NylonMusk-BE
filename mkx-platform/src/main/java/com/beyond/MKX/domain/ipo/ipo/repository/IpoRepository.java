package com.beyond.MKX.domain.ipo.ipo.repository;

import com.beyond.MKX.domain.ipo.ipo.entity.Ipo;
import com.beyond.MKX.domain.ipo.ipo.entity.IpoStatus;
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
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IpoRepository extends JpaRepository<Ipo, UUID> {
    boolean existsBySymbol(String symbol);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Ipo i where i.id = :id")
    Optional<Ipo> findByIdForUpdate(UUID id);

//    No FetchJoin
//    @Query("""
//      select i
//      from Ipo i
//      where i.status in :statuses
//        and (:q is null
//             or lower(i.symbol) like lower(concat('%', :q, '%'))
//             or lower(i.corporation.name) like lower(concat('%', :q, '%')))
//        and (:from is null or i.requestedAt >= :from)
//        and (:to   is null or i.requestedAt <  :to)
//      order by i.requestedAt desc
//    """)
//    Page<Ipo> searchRequests(
//            @Param("statuses") Collection<IpoStatus> statuses,
//            @Param("q") String q,
//            @Param("from") LocalDateTime from,
//            @Param("to") LocalDateTime to,
//            Pageable pageable
//    );

//    fetchJoin : N+1 해결
    @Query(
            value = """
    select i
    from Ipo i
      join fetch i.corporation c
    where i.status in :statuses
      and (:q is null
           or lower(i.symbol) like lower(concat('%', :q, '%'))
           or lower(c.name)   like lower(concat('%', :q, '%')))
      and (:from is null or i.requestedAt >= :from)
      and (:to   is null or i.requestedAt <  :to)
    """,
            countQuery = """
    select count(i)
    from Ipo i
      join i.corporation c
    where i.status in :statuses
      and (:q is null
           or lower(i.symbol) like lower(concat('%', :q, '%'))
           or lower(c.name)   like lower(concat('%', :q, '%')))
      and (:from is null or i.requestedAt >= :from)
      and (:to   is null or i.requestedAt <  :to)
    """
    )
    Page<Ipo> searchRequests(
            @Param("statuses") Collection<IpoStatus> statuses,
            @Param("q") String q,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );
}
