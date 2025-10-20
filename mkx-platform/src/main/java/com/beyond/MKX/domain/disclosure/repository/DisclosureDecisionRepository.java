package com.beyond.MKX.domain.disclosure.repository;

import com.beyond.MKX.domain.disclosure.entity.DisclosureDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DisclosureDecisionRepository extends JpaRepository<DisclosureDecision, UUID> {
}

