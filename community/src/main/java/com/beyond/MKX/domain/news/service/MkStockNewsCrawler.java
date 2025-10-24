package com.beyond.MKX.domain.news.service;

import com.beyond.MKX.domain.news.dto.NewsReqDto;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class MkStockNewsCrawler {

    public List<NewsReqDto> fetchStockNews(String keyword) {
        List<NewsReqDto> result = new ArrayList<>();

        try {
            String encodedKeyword = URLEncoder.encode(keyword + " site:mk.co.kr", StandardCharsets.UTF_8);
            String rssUrl = "https://news.google.com/rss/search?q=" + encodedKeyword + "&hl=ko&gl=KR&ceid=KR:ko";

            Document doc = Jsoup.connect(rssUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(8000)
                    .get();

            Elements items = doc.select("item");
            log.info("[{}] 관련 기사 수: {}", keyword, items.size());

            for (Element item : items) {
                String title = item.selectFirst("title").text();
                String link = item.selectFirst("link").text();
                String pubDate = item.selectFirst("pubDate").text();

                LocalDateTime publishedAt;
                try {
                    publishedAt = ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME).toLocalDateTime();
                } catch (Exception e1) {
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss XXX", Locale.ENGLISH);
                        publishedAt = ZonedDateTime.parse(pubDate, formatter).toLocalDateTime();
                    } catch (Exception e2) {
                        log.warn("날짜 파싱 실패: {}", pubDate);
                        publishedAt = LocalDateTime.now();
                    }
                }

                result.add(NewsReqDto.builder()
                        .title(title)
                        .sourceName("매일경제")
                        .sourceUrl(link)
                        .publishedAt(publishedAt)
                        .build());
            }

        } catch (Exception e) {
            log.error("매일경제 뉴스 크롤링 실패", e);
        }

        return result;
    }
}



