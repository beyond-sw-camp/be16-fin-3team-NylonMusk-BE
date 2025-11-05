package com.beyond.MKX.domain.stockfavorite.repository;

import com.beyond.MKX.domain.stock.entity.Stock;
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
        SELECT f.stock
        FROM StockFavorites f
        WHERE f.member.id = :memberId
    """)
    List<Stock> findFavoriteStocks(@Param("memberId") UUID memberId);

    /**
     * 인기 종목 TOP N 조회
     * 
     * 즐겨찾기가 많은 순서대로 정렬
     */
    @Query("""
        SELECT f.stock
        FROM StockFavorites f
        GROUP BY f.stock
        ORDER BY COUNT(f.stock) DESC
        LIMIT :limit
    """)
    List<Stock> findTopPopularStocks(@Param("limit") int limit);
}
