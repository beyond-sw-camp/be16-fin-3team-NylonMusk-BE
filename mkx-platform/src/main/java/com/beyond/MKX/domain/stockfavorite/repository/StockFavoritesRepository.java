package com.beyond.MKX.domain.stockfavorite.repository;

import com.beyond.MKX.domain.stockfavorite.dto.StockFavoritesResDTO;
import com.beyond.MKX.domain.stockfavorite.entity.StockFavorites;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface StockFavoritesRepository extends JpaRepository<StockFavorites, UUID> {
    boolean existsByMember_IdAndStock_Id(UUID memberId, UUID stockId);

    void deleteByMember_IdAndStock_Id(UUID memberId, UUID stockId);

    List<StockFavorites> findAllByMember_Id(UUID memberId);

    @Query("""
            SELECT StockFavoritesResDTO(
            s.id, s.nameKo, s.ticker
            )
            FROM StockFavorites f
            JOIN f.stock s
            WHERE f.member.id = :memberId
            """)
    List<StockFavoritesResDTO> findFavoritesWithStock(@Param("memberId") UUID memberId);
}
