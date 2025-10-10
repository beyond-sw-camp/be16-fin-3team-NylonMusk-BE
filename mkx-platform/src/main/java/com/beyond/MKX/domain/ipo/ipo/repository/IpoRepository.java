package com.beyond.MKX.domain.ipo.ipo.repository;

import com.beyond.MKX.domain.ipo.ipo.entity.Ipo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IpoRepository extends JpaRepository<Ipo, UUID> {
    boolean existsBySymbol(String symbol);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Ipo i where i.id = :id")
    Optional<Ipo> findByIdForUpdate(UUID id);
}
