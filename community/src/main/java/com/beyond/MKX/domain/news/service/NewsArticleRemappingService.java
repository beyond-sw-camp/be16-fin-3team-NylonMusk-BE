package com.beyond.MKX.domain.news.service;

import com.beyond.MKX.domain.news.entity.NewsArticle;
import com.beyond.MKX.domain.news.entity.NewsArticleStock;
import com.beyond.MKX.domain.news.repository.NewsArticleRepository;
import com.beyond.MKX.domain.news.repository.NewsArticleStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsArticleRemappingService {

    private final NewsArticleRepository newsArticleRepository;
    private final NewsArticleStockRepository articleStockRepository;

    /**
     * 상장된 종목의 기존 뉴스와 재매핑
     * @param stockId 상장된 종목 ID
     * @param stockNameKo 종목명 (한글)
     * @param ticker 종목 코드
     * @return 매칭된 뉴스 개수
     */
    @Transactional
    public int remapNewsForListedStock(UUID stockId, String stockNameKo, String ticker) {
        if (stockNameKo == null || stockNameKo.isBlank()) {
            log.warn("[뉴스 재매핑] 종목명이 없어 재매핑을 건너뜁니다. stockId={}", stockId);
            return 0;
        }

        String normalizedStockName = normalizeKo(stockNameKo);
        log.info("[뉴스 재매핑] 시작: stockId={}, ticker={}, nameKo={}", stockId, ticker, stockNameKo);

        int page = 0;
        int size = 100;
        int matchedCount = 0;

        // 모든 뉴스를 페이지네이션으로 순회하며 재매핑
        while (true) {
            Pageable pageable = PageRequest.of(page, size);
            Page<NewsArticle> newsPage = newsArticleRepository.findAll(pageable);

            if (newsPage.isEmpty()) {
                break;
            }

            for (NewsArticle article : newsPage.getContent()) {
                // 이미 매핑되어 있는지 확인
                if (articleStockRepository.existsByArticleAndStockId(article, stockId)) {
                    continue;
                }

                // 제목/본문에서 종목명이 포함되어 있는지 확인
                String normalizedTitle = normalizeKo(article.getTitle());
                String normalizedDescription = normalizeKo(article.getDescription());

                boolean isMatched = (normalizedTitle != null && normalizedTitle.contains(normalizedStockName)) ||
                                   (normalizedDescription != null && normalizedDescription.contains(normalizedStockName));

                if (isMatched) {
                    // 매핑 정보 저장
                    articleStockRepository.save(NewsArticleStock.builder()
                            .article(article)
                            .stockId(stockId)
                            .ticker(ticker)
                            .stockName(stockNameKo)
                            .build());
                    matchedCount++;
                    
                    log.debug("[뉴스 재매핑] 매핑 추가: articleId={}, title={}", 
                            article.getId(), article.getTitle());
                }
            }

            if (newsPage.isLast()) {
                break;
            }
            page++;
        }

        log.info("[뉴스 재매핑] 완료: stockId={}, ticker={}, 매칭된 뉴스 수={}", 
                stockId, ticker, matchedCount);
        return matchedCount;
    }

    /**
     * 종목명 정규화 (RssNewsCrawlerService와 동일한 로직)
     */
    private String normalizeKo(String s) {
        if (s == null) return null;
        // 공백/특수문자 제거, 괄호/따옴표 등 제거
        return s.replaceAll("[\\s\\p{Punct}]", "")
                .replace("·", "")
                .replace("ㆍ", "")
                .toLowerCase();
    }
}
