package com.beyond.MKX.domain.disclosure.repository;

import com.beyond.MKX.domain.disclosure.entity.DisclosureSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface DisclosureSequenceRepository extends JpaRepository<DisclosureSequence, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DisclosureSequence> findByYear(int year);
}

