package com.beyond.MKX.domain.stockfavorite.repository;

import com.beyond.MKX.domain.stockfavorite.entity.StockFavorites;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StockFavoritesRepository extends JpaRepository<StockFavorites, UUID> {
    
}
