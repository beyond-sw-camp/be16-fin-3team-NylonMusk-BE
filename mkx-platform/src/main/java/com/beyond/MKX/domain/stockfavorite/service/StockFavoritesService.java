package com.beyond.MKX.domain.stockfavorite.service;

import com.beyond.MKX.domain.member.entity.Member;
import com.beyond.MKX.domain.member.repository.MemberRepository;
import com.beyond.MKX.domain.stock.entity.Stock;
import com.beyond.MKX.domain.stock.repository.StockRepository;
import com.beyond.MKX.domain.stockfavorite.entity.StockFavorites;
import com.beyond.MKX.domain.stockfavorite.repository.StockFavoritesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class StockFavoritesService {
    private final StockFavoritesRepository favoritesRepository;
    private final StockRepository stockRepository;
    private final MemberRepository memberRepository;

    public void addFavorite(UUID memberId, UUID stockId) {
        if (favoritesRepository.existsByMember_IdAndStock_Id(memberId, stockId)) {
            throw new IllegalArgumentException("이미 즐겨찾기 되어 있는 종목입니다.");
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new IllegalArgumentException("종목 없음"));

        favoritesRepository.save(StockFavorites.builder()
                .member(member)
                .stock(stock)
                .build());
    }

    public void removeFavorite(UUID memberId, UUID stockId) {
        favoritesRepository.deleteByMember_IdAndStock_Id(memberId, stockId);
    }

    @Transactional(readOnly = true)
    public List<UUID> getFavoritesStockIds(UUID memberId) {
        return favoritesRepository.findFavoriteStockIds(memberId);
    }
}
