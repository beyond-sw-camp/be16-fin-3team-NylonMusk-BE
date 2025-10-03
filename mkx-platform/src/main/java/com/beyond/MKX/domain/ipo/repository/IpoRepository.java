package com.beyond.MKX.domain.ipo.repository;

import com.beyond.MKX.domain.ipo.entity.Ipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IpoRepository extends JpaRepository<Ipo, UUID> {
    boolean existsBySymbol(String symbol);
}
