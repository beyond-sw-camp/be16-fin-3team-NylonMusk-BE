package com.beyond.MKX.domain.execution.repository;

import com.beyond.MKX.domain.execution.entity.Ledger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LedgerRepository extends JpaRepository<Ledger, UUID> {
}
