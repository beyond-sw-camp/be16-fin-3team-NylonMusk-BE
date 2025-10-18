package com.beyond.MKX.domain.disclosure.repository;

import com.beyond.MKX.domain.disclosure.entity.Disclosure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Optional;

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
}
