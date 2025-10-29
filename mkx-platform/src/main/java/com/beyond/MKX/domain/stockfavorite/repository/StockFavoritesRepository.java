package com.beyond.MKX.domain.stockfavorite.repository;

import com.beyond.MKX.domain.stockfavorite.entity.StockFavorites;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StockFavoritesRepository extends JpaRepository<StockFavorites, UUID> {
    boolean existsByMember_IdAndStock_Id(UUID memberId, UUID stockId);

    void deleteByMember_IdAndStock_Id(UUID memberId, UUID stockId);

    List<StockFavorites> findAllByMember_Id(UUID memberId);
}
