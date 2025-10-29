package com.beyond.MKX.domain.stockfavorite.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.common.auth.security.CustomMemberPrincipal;
import com.beyond.MKX.domain.stockfavorite.dto.StockFavoritesResDTO;
import com.beyond.MKX.domain.stockfavorite.entity.StockFavorites;
import com.beyond.MKX.domain.stockfavorite.service.StockFavoritesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/stocks/favorites")
@RequiredArgsConstructor
public class StockFavoritesController {

    private final StockFavoritesService favoritesService;

    @PostMapping("{stockId}")
    public ResponseEntity<?> addFavorite(@PathVariable UUID stockId, @AuthenticationPrincipal CustomMemberPrincipal principal) {
        StockFavoritesResDTO dto = favoritesService.addFavorite(principal.id(), stockId);
        return ApiResponse.ok(dto, "종목 즐겨찾기 완료");
    }

    @DeleteMapping("{stockId}")
    public ResponseEntity<?> deleteFavorite(@PathVariable UUID stockId, @AuthenticationPrincipal CustomMemberPrincipal principal) {
        favoritesService.removeFavorite(principal.id(), stockId);
        return ApiResponse.ok("즐겨찾기 삭제 완료");
    }

    @GetMapping
    public ResponseEntity<?> getFavorites(@AuthenticationPrincipal CustomMemberPrincipal principal) {
        List<StockFavoritesResDTO> list = favoritesService.getFavorites(principal.id())
                .stream()
                .map(f -> StockFavoritesResDTO.builder()
                        .stockId(f.getStock().getId())
                        .nameKo(f.getStock().getNameKo())
                        .ticker(f.getStock().getTicker())
                        .build())
                .toList();

        return ApiResponse.ok(list, "즐겨찾기 목록 조회 완료");
    }
}
