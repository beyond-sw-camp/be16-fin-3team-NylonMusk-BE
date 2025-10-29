package com.beyond.MKX.domain.stockfavorite.repository;

import com.beyond.MKX.domain.stockfavorite.entity.StockFavorites;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface StockFavoritesRepository extends JpaRepository<StockFavorites, UUID> {
    boolean existsByMember_IdAndStock_Id(UUID memberId, UUID stockId);

    void deleteByMember_IdAndStock_Id(UUID memberId, UUID stockId);

    /** 즐겨찾기 종목 ID 목록만 반환 */
    @Query("""
        SELECT f.stock.id
        FROM StockFavorites f
        WHERE f.member.id = :memberId
    """)
    List<UUID> findFavoriteStockIds(@Param("memberId") UUID memberId);
}
