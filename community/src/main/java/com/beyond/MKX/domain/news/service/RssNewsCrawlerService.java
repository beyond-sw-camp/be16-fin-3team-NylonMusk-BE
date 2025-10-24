package com.beyond.MKX.domain.news.service;

import com.beyond.MKX.domain.news.client.StockSearchClient;
import com.beyond.MKX.domain.news.entity.NewsArticle;
import com.beyond.MKX.domain.news.repository.NewsArticleRepository;
import com.beyond.MKX.domain.news.repository.NewsArticleStockRepository;
import com.beyond.MKX.domain.news.entity.NewsArticleStock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URI;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class RssNewsCrawlerService {

    private final NewsArticleRepository newsArticleRepository;
    private final StockSearchClient stockClient;
    private final NewsArticleStockRepository articleStockRepository;

    // OpenAI 직접 호출 (내부)
    @Value("${openai.api-key:}")
    private String openAiApiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String openAiModel;

    private final WebClient openAiClient = WebClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final List<String> RSS_FEEDS = List.of(
            "https://www.mk.co.kr/rss/30100041/",
            "https://www.hankyung.com/feed/finance",
            "https://www.chosun.com/arc/outboundfeeds/rss/category/economy/?outputType=xml"
    );

    private static final DateTimeFormatter RFC1123 = DateTimeFormatter.RFC_1123_DATE_TIME;

    @Scheduled(fixedDelayString = "${news.rss.fixed-delay-ms:60000}")
    @Transactional
    public void crawlLatest() {
        try {
            Map<String, StockInfo> stockMap = loadListedStocks();
            for (String feed : RSS_FEEDS) {
                processFeed(feed, stockMap);
            }
        } catch (Exception e) {
            log.error("RSS 크롤링 실패", e);
        }
    }

    private Map<String, StockInfo> loadListedStocks() {
        Map<String, StockInfo> map = new HashMap<>();
        try {
            int page = 0;
            int size = 100;
            while (true) {
                var res = stockClient.search(null, "LISTED", page, size);
                if (res == null || res.getContent() == null || res.getContent().isEmpty()) break;
                res.getContent().forEach(it -> map.put(it.getNameKo(), new StockInfo(it.getId(), it.getTicker(), it.getNameKo())));
                if (res.isLast()) break;
                page += 1;
            }
            log.info("LISTED 종목 로드: {}건", map.size());
        } catch (Exception e) {
            log.warn("LISTED 종목 조회 실패: {}", e.getMessage());
        }
        return map;
    }

    private void processFeed(String feedUrl, Map<String, StockInfo> stockMap) {
        try {
            Document doc = Jsoup.connect(feedUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();
            Elements items = doc.select("item");
            log.info("RSS [{}] 아이템: {}건", feedUrl, items.size());
            String defaultPublisher = publisherFromFeed(feedUrl);
            for (Element item : items) {
                String title = text(item.selectFirst("title"));
                String link = text(item.selectFirst("link"));
                String pubDate = text(item.selectFirst("pubDate"));
                String descriptionRaw = text(item.selectFirst("description"));
                String publisher = text(item.selectFirst("source"));
                // Jsoup CSS 쿼리에서 네임스페이스 콜론은 사용 불가 → 태그 탐색으로 처리
                String author = firstNonNull(
                        text(item.selectFirst("author")),
                        firstByTag(item, "dc:creator"),
                        text(item.selectFirst("creator"))
                );

                LocalDateTime publishedAt = parseDate(pubDate);
                String description = htmlToText(descriptionRaw);
                String thumbnail = extractRssThumbnail(item);
                if (thumbnail == null) {
                    String fromDesc = extractImgFromHtml(descriptionRaw);
                    if (fromDesc != null) thumbnail = fromDesc;
                }

                // publisher 폴백: 링크 호스트로 사람이 읽기 쉬운 이름 매핑
                if (publisher == null || publisher.isBlank()) {
                    publisher = defaultPublisher;
                }

                // description 폴백: 없으면 제목 사용
                if (description == null || description.isBlank()) {
                    description = title;
                }

                if (link == null || link.isBlank()) continue;
                if (newsArticleRepository.existsByLink(link)) continue;

                // 종목 매칭: 제목/본문에 nameKo 포함 시 전부 수집
                List<StockInfo> matched = new ArrayList<>();
                for (Map.Entry<String, StockInfo> e : stockMap.entrySet()) {
                    String name = e.getKey();
                    if ((title != null && title.contains(name)) || (description != null && description.contains(name))) {
                        matched.add(e.getValue());
                    }
                }

                // 매칭된 종목이 하나도 없으면 저장하지 않음
                if (matched.isEmpty()) continue;

                // 첫 매칭은 편의상 article의 ticker/stockId에 기록
                UUID stockId = matched.get(0).id();
                String ticker = matched.get(0).ticker();

                NewsArticle article = NewsArticle.builder()
                        .title(title)
                        .link(link)
                        .publisher(publisher)
                        .author(author)
                        .publishedAt(publishedAt)
                        .description(description)
                        .thumbnailUrl(thumbnail)
                        .stockId(stockId)
                        .ticker(ticker)
                        .build();
                newsArticleRepository.save(article);

                // 다중 종목 매핑 저장 (중복 방지)
                for (StockInfo s : matched) {
                    if (!articleStockRepository.existsByArticleAndStockId(article, s.id())) {
                        articleStockRepository.save(NewsArticleStock.builder()
                                .article(article)
                                .stockId(s.id())
                                .ticker(s.ticker())
                                .stockName(s.nameKo())
                                .build());
                    }
                }

                // OpenAI 분석 (요약/감정) - 실패 시 무시
                analyzeWithOpenAI(article);

            }
        } catch (Exception e) {
            log.warn("RSS 처리 실패 [{}]: {}", feedUrl, e.getMessage());
        }
    }

    private static String text(Element el) {
        return el == null ? null : el.text();
    }

    private static String firstByTag(Element el, String tag) {
        if (el == null || tag == null) return null;
        try {
            Element found = el.getElementsByTag(tag).first();
            return found != null ? found.text() : null;
        } catch (Exception ignore) { return null; }
    }

    private static String firstNonNull(String... values) {
        if (values == null) return null;
        for (String v : values) { if (v != null && !v.isBlank()) return v; }
        return null;
    }

    private static String htmlToText(String html) {
        if (html == null || html.isBlank()) return null;
        try { return Jsoup.parse(html).text(); } catch (Exception ignore) { return html; }
    }

    private static String extractRssThumbnail(Element item) {
        if (item == null) return null;
        // media:thumbnail, media:content, enclosure url
        Element mediaThumb = item.selectFirst("thumbnail, media\\:thumbnail, media\\:content");
        if (mediaThumb != null) {
            String url = mediaThumb.hasAttr("url") ? mediaThumb.attr("url") : mediaThumb.attr("abs:url");
            if (url != null && !url.isBlank()) return url;
        }
        Element enclosure = item.selectFirst("enclosure[url]");
        if (enclosure != null) {
            String type = enclosure.attr("type");
            if (type != null && type.startsWith("image")) return enclosure.attr("url");
        }
        return null;
    }

    private static String extractImgFromHtml(String html) {
        if (html == null) return null;
        try {
            Document doc = Jsoup.parse(html);
            Element img = doc.selectFirst("img[src]");
            if (img != null) return img.absUrl("src");
        } catch (Exception ignore) {}
        return null;
    }

    private static LocalDateTime parseDate(String pubDate) {
        if (pubDate == null || pubDate.isBlank()) return LocalDateTime.now();
        try { return ZonedDateTime.parse(pubDate, RFC1123).toLocalDateTime(); } catch (Exception ignore) {}
        try { return ZonedDateTime.parse(pubDate).toLocalDateTime(); } catch (Exception ignore) {}
        return LocalDateTime.now();
    }

    private record StockInfo(UUID id, String ticker, String nameKo) {}

    private void analyzeWithOpenAI(NewsArticle article) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) return;
        try {
            Map<String, Object> body = Map.of(
                    "model", openAiModel,
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are a Korean financial news assistant. Respond JSON only."),
                            Map.of("role", "user", "content", buildPrompt(article))
                    ),
                    "temperature", 0.2
            );
            String res = openAiClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            if (res == null || res.isBlank()) return;
            JsonNode top = objectMapper.readTree(res);
            JsonNode msg = top.path("choices").get(0).path("message").path("content");
            String content = msg.asText("").replace("```json", "").replace("```", "").trim();
            if (content.isBlank()) return;
            JsonNode root = objectMapper.readTree(content);
            String summary = root.path("summary").asText(null);
            String sentiment = root.path("sentiment").asText(null);
            if (summary != null && !summary.isBlank()) article.setSummary(summary);
            if (sentiment != null && !sentiment.isBlank()) article.setSentiment(sentiment.toUpperCase());
            // JPA dirty checking으로 저장됨(@Transactional)
        } catch (WebClientResponseException e) {
            // 429/4xx/5xx 무시
        } catch (Exception ignore) {
        }
    }

    private String buildPrompt(NewsArticle a) {
        return String.format("""
                다음 한국 경제 기사에 대해 JSON으로만 답하세요.
                필드:
                - summary: 2문장 요약
                - sentiment: POSITIVE|NEGATIVE|NEUTRAL 중 하나

                제목: %s
                언론사: %s
                기자: %s
                본문: %s
                """,
                nullToEmpty(a.getTitle()), nullToEmpty(a.getPublisher()), nullToEmpty(a.getAuthor()), nullToEmpty(a.getDescription())
        );
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    private String publisherFromFeed(String feedUrl) {
        try {
            String host = URI.create(feedUrl).getHost();
            if (host == null) return null;
            String h = host.replaceFirst("^www\\.", "");
            if (h.endsWith("mk.co.kr")) return "매일경제";
            if (h.endsWith("hankyung.com")) return "한국경제";
            if (h.endsWith("chosun.com")) return "조선일보";
            return host;
        } catch (Exception e) {
            return null;
        }
    }
}
