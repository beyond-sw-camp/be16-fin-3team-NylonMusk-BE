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
import org.jsoup.parser.Parser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    @Scheduled(fixedDelayString = "${news.rss.fixed-delay-ms}")
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
            if (map.isEmpty()) {
                log.warn("LISTED 종목이 0건입니다. mkx-platform-service 실행 여부/Eureka 등록을 확인하세요.");
            }
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
                    // RSS는 네임스페이스(dc:creator 등)를 포함하므로 XML 파서 사용
                    .parser(Parser.xmlParser())
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

                // publisher 폴백: 피드 호스트 기반 기본값 사용
                if (publisher == null || publisher.isBlank()) {
                    publisher = defaultPublisher;
                }

                // 썸네일이 아직 없으면 본문에서 첫 이미지 추출 (매일경제/한국경제/조선일보만)
                if ((thumbnail == null || thumbnail.isBlank()) && 
                    ("매일경제".equals(publisher) || "한국경제".equals(publisher) || "조선일보".equals(publisher))) {
                    String fromArticle = fetchArticleImage(link);
                    if (fromArticle != null) thumbnail = fromArticle;
                }

                // description 폴백: 없으면 제목 사용
                if (description == null || description.isBlank()) {
                    description = title;
                }

                // author 정규화: 언론사명과 동일하거나 브랜드명이면 무시 후 본문/제목에서 기자명 추출 시도
                author = normalizeAuthor(author, publisher, description, title);
                // 매일경제/한국경제/조선일보는 페이지에서 author 메타/바이라인 추가 시도
                if (author == null && ("매일경제".equals(publisher) || "한국경제".equals(publisher) || "조선일보".equals(publisher))) {
                    author = fetchAuthorFromArticle(link);
                    if (author != null && author.length() > 100) {
                        author = author.substring(0, 100);
                    }
                }
                // 이메일 제거/형태 정리 및 매일경제 케이스 '기자' 접미어 보강
                author = postProcessAuthor(author, publisher);

                if (link == null || link.isBlank()) continue;
                if (newsArticleRepository.existsByLink(link)) continue;

                // 종목 매칭: 제목/본문에 nameKo 포함 시 전부 수집
                List<StockInfo> matched = new ArrayList<>();
                String normTitle = normalizeKo(title);
                String normDesc = normalizeKo(description);
                for (Map.Entry<String, StockInfo> e : stockMap.entrySet()) {
                    String name = e.getKey();
                    String normName = normalizeKo(name);
                    if ((normTitle != null && normTitle.contains(normName)) || (normDesc != null && normDesc.contains(normName))) {
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
                log.info("저장: [{}] {} (매칭 {}건, 첫 종목: {}{})",
                        publisher,
                        title,
                        matched.size(),
                        ticker,
                        matched.size() > 1 ? ", 외 " + (matched.size()-1) + "건" : "");

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

                // OpenAI 분석 (요약) - 재시도 로직 포함
                analyzeWithOpenAIWithRetry(article, 3);

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

    private String normalizeKo(String s) {
        if (s == null) return null;
        // 공백/특수문자 제거, 괄호/따옴표 등 제거
        return s.replaceAll("[\\s\\p{Punct}]", "")
                .replace("·", "")
                .replace("ㆍ", "")
                .toLowerCase();
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

    /**
     * OpenAI로 요약 생성 (재시도 포함)
     * @param article 뉴스 기사
     * @param maxRetries 최대 재시도 횟수
     * @return 요약 생성 성공 여부
     */
    private boolean analyzeWithOpenAIWithRetry(NewsArticle article, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            if (analyzeWithOpenAI(article)) {
                return true; // 성공
            }
            
            if (attempt < maxRetries) {
                // 재시도 전 대기 (exponential backoff: 1초, 2초, 4초...)
                long waitTime = (long) Math.pow(2, attempt - 1) * 1000;
                log.info("[OpenAI 요약] 재시도 대기: articleId={}, attempt={}/{}, wait={}ms", 
                        article.getId(), attempt + 1, maxRetries, waitTime);
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        log.warn("[OpenAI 요약] 최대 재시도 횟수 초과: articleId={}, title={}", article.getId(), article.getTitle());
        return false;
    }

    /**
     * OpenAI로 요약 생성 (단일 시도)
     * @param article 뉴스 기사
     * @return 요약 생성 성공 여부
     */
    private boolean analyzeWithOpenAI(NewsArticle article) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            log.debug("[OpenAI 요약] API 키가 설정되지 않아 요약 생성을 건너뜁니다. articleId={}", article.getId());
            return false;
        }
        try {
            log.debug("[OpenAI 요약] 시작: articleId={}, title={}", article.getId(), article.getTitle());
            Map<String, Object> body = Map.of(
                    "model", openAiModel,
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are a Korean financial news assistant. Respond JSON only."),
                            Map.of("role", "user", "content", buildPrompt(article))
                    ),
                    "temperature", 0.2,
                    "max_tokens", 800
            );
            String res = openAiClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            if (res == null || res.isBlank()) {
                log.warn("[OpenAI 요약] 응답이 비어있습니다. articleId={}", article.getId());
                return false;
            }
            JsonNode top = objectMapper.readTree(res);
            JsonNode choices = top.path("choices");
            if (choices.isEmpty() || !choices.isArray()) {
                log.warn("[OpenAI 요약] choices가 없거나 배열이 아닙니다. articleId={}, response={}", article.getId(), res.substring(0, Math.min(200, res.length())));
                return false;
            }
            JsonNode msg = choices.get(0).path("message").path("content");
            String content = msg.asText("").replace("```json", "").replace("```", "").trim();
            if (content.isBlank()) {
                log.warn("[OpenAI 요약] 메시지 content가 비어있습니다. articleId={}", article.getId());
                return false;
            }
            JsonNode root = objectMapper.readTree(content);
            JsonNode summaryNode = root.path("summary");
            String summary = null;
            
            // summary가 배열인 경우와 문자열인 경우 모두 처리
            if (summaryNode.isArray()) {
                // 배열인 경우 각 요소를 문장으로 합침
                StringBuilder sb = new StringBuilder();
                for (JsonNode item : summaryNode) {
                    String text = item.asText("");
                    if (text != null && !text.isBlank()) {
                        if (sb.length() > 0) {
                            sb.append(" ");
                        }
                        sb.append(text);
                    }
                }
                summary = sb.toString();
                log.debug("[OpenAI 요약] summary 배열을 문자열로 변환: articleId={}, items={}", 
                        article.getId(), summaryNode.size());
            } else if (summaryNode.isTextual()) {
                // 문자열인 경우
                summary = summaryNode.asText(null);
            } else if (!summaryNode.isNull() && !summaryNode.isMissingNode()) {
                // 다른 타입인 경우 문자열로 변환 시도
                summary = summaryNode.asText(null);
            }
            
            if (summary != null && !summary.isBlank()) {
                article.setSummary(summary);
                log.info("[OpenAI 요약] 성공: articleId={}, summaryLength={}", article.getId(), summary.length());
                // JPA dirty checking으로 저장됨(@Transactional)
                return true;
            } else {
                log.warn("[OpenAI 요약] summary 필드가 없거나 비어있습니다. articleId={}, summaryNodeType={}, content={}", 
                        article.getId(), summaryNode.getNodeType(), content.substring(0, Math.min(500, content.length())));
                return false;
            }
        } catch (WebClientResponseException e) {
            // 429 (Too Many Requests)는 재시도 대상, 다른 4xx/5xx는 재시도 불가
            if (e.getStatusCode().value() == 429) {
                log.warn("[OpenAI 요약] Rate limit 초과 (재시도 가능): articleId={}, status={}", article.getId(), e.getStatusCode());
            } else {
                log.warn("[OpenAI 요약] HTTP 오류 (재시도 불가): articleId={}, status={}, message={}", 
                        article.getId(), e.getStatusCode(), e.getMessage());
            }
            return false;
        } catch (Exception e) {
            log.error("[OpenAI 요약] 요약 생성 실패: articleId={}, error={}", article.getId(), e.getMessage(), e);
            return false;
        }
    }

    private String buildPrompt(NewsArticle a) {
        return String.format("""
                다음 한국 경제 기사에 대해 JSON으로만 답하세요.
                필드:
                - summary: 반드시 정확히 10문장으로 구성된 상세한 요약. 각 문장은 핵심 내용을 포함해야 하며, 문장 수를 정확히 10개로 맞춰주세요. 요약은 기사의 주요 내용, 배경, 의미 등을 포괄적으로 다뤄야 합니다.

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

    private String normalizeAuthor(String author, String publisher, String description, String title) {
        String a = (author == null) ? null : author.trim();
        if (a != null && a.equalsIgnoreCase(publisher)) a = null;
        if (a != null && isBrandName(a)) a = null;
        if (a == null) {
            String fromText = extractAuthorFromText(description);
            if (fromText == null || fromText.isBlank()) {
                fromText = extractAuthorFromText(title);
            }
            a = fromText;
        }
        if (a != null && a.length() > 100) {
            a = a.substring(0, 100);
        }
        return a;
    }

    private boolean isBrandName(String v) {
        if (v == null) return false;
        // 이름 문자열이 브랜드명 '그 자체'인 경우만 true 처리 (포함 여부는 제외)
        String s = v.replaceAll("\\s+", "");
        s = s.replaceAll("기자$", "");
        String[] brands = { "매일경제", "매경", "매경닷컴", "한국경제", "한경", "조선일보", "조선비즈" };
        for (String b : brands) {
            if (s.equalsIgnoreCase(b)) return true;
        }
        return false;
    }

    private String extractAuthorFromText(String text) {
        if (text == null) return null;
        try {
            Pattern p = Pattern.compile("([가-힣]{2,4})\\s*기자");
            Matcher m = p.matcher(text);
            if (m.find()) {
                return m.group(1) + " 기자";
            }
        } catch (Exception ignore) {}
        return null;
    }

    // 매일경제/한국경제/조선일보 기사 페이지에서 기자명 추출(메타/바이라인)
    private String fetchAuthorFromArticle(String link) {
        try {
            Document doc = Jsoup.connect(link)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .referrer("https://www.google.com/")
                    .timeout(5000)
                    .get();
            // 1) 메타 태그 시도
            Element meta = doc.selectFirst(
                    "meta[name=byline], meta[property=article:author], meta[name=author], meta[property=og:author]"
            );
            if (meta != null) {
                String a = meta.hasAttr("content") ? meta.attr("content") : meta.attr("value");
                if (a != null && !a.isBlank() && !isBrandName(a)) return a.trim();
            }
            // 2) 바이라인/기자 요소 시도
            Element by = doc.selectFirst(
                    ".author, .byline, .reporter, .journalist, .writer, [class*=reporter], [class*=author], [class*=byline], [itemprop=author], [rel=author]"
            );
            if (by != null) {
                String a = by.text();
                if (a != null && !a.isBlank() && !isBrandName(a)) return a.trim();
            }
            // 3) 텍스트 패턴
            String fromText = extractAuthorFromText(doc.text());
            if (fromText != null && !fromText.isBlank()) return fromText;
        } catch (Exception ignore) {}
        return null;
    }

    // 기사 페이지에서 본문 첫 이미지 추출 (썸네일용)
    private String fetchArticleImage(String link) {
        try {
            Document doc = Jsoup.connect(link)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .referrer("https://www.google.com/")
                    .timeout(5000)
                    .get();
            
            // 본문 영역 찾기 (여러 셀렉터 시도)
            String[] contentSelectors = {
                "article", 
                ".article-content", 
                ".news_view", 
                ".article_view", 
                ".article_body", 
                "#articleBody", 
                ".news_cnt_detail_wrap",
                "[class*=article]",
                "[class*=content]",
                "[id*=article]",
                "[id*=content]",
                "main",
                ".main-content"
            };
            
            Element contentEl = null;
            for (String selector : contentSelectors) {
                contentEl = doc.selectFirst(selector);
                if (contentEl != null) {
                    log.debug("본문 영역 발견 (selector={}): {}", selector, link);
                    break;
                }
            }
            
            // 본문 영역이 있으면 본문 내에서 이미지 찾기
            if (contentEl != null) {
                String image = findImageInElement(contentEl, link, true);
                if (image != null) {
                    return image;
                }
                log.debug("본문 영역 내에서 이미지를 찾지 못함, 전체 문서에서 시도: {}", link);
            } else {
                log.warn("본문 영역을 찾을 수 없음, 전체 문서에서 이미지 검색 시도: {}", link);
            }
            
            // 본문 영역이 없거나 본문 내에서 찾지 못한 경우 전체 문서에서 이미지 찾기 (폴백)
            String image = findImageInElement(doc.body(), link, false);
            if (image != null) {
                log.info("전체 문서에서 이미지 추출 성공: {}", image);
                return image;
            }
            
            log.warn("본문에 적절한 이미지를 찾을 수 없음: {}", link);
        } catch (Exception e) {
            log.warn("기사 이미지 추출 실패 [{}]: {}", link, e.getMessage());
        }
        return null;
    }
    
    /**
     * 요소 내에서 적절한 이미지 찾기
     * @param element 검색할 요소
     * @param link 기사 링크 (로깅용)
     * @param strict 본문 영역에서 검색하는 경우 true (더 엄격한 필터링)
     * @return 이미지 URL 또는 null
     */
    private String findImageInElement(Element element, String link, boolean strict) {
        if (element == null) return null;
        
        Elements images = element.select("img[src]");
        for (Element img : images) {
            String url = img.absUrl("src");
            if (url == null || !url.startsWith("http")) continue;
            
            // icon, logo, button, avatar 등 제외
            String urlLower = url.toLowerCase();
            if (urlLower.contains("icon") || urlLower.contains("logo") || 
                urlLower.contains("button") || urlLower.contains("avatar") ||
                urlLower.contains("profile") || urlLower.contains("thumbnail") && !urlLower.contains("article")) {
                continue;
            }
            
            // 너비가 너무 작은 이미지 제외
            String width = img.attr("width");
            if (width != null && !width.isEmpty()) {
                try {
                    int w = Integer.parseInt(width);
                    if (w < (strict ? 200 : 150)) continue; // 본문 영역에서는 더 큰 이미지만
                } catch (NumberFormatException ignore) {}
            }
            
            // data-src 속성도 확인 (lazy loading)
            if (url.contains("data:image") || url.contains("placeholder")) {
                String dataSrc = img.attr("data-src");
                if (dataSrc != null && !dataSrc.isBlank() && dataSrc.startsWith("http")) {
                    url = img.absUrl("data-src");
                } else {
                    continue;
                }
            }
            
            // class나 id에 불필요한 키워드가 있는지 확인
            String imgClass = img.attr("class");
            String imgId = img.attr("id");
            if ((imgClass != null && (imgClass.contains("icon") || imgClass.contains("logo") || imgClass.contains("button"))) ||
                (imgId != null && (imgId.contains("icon") || imgId.contains("logo") || imgId.contains("button")))) {
                continue;
            }
            
            log.info("이미지 추출 성공: {} (strict={})", url, strict);
            return url;
        }
        return null;
    }

    private String postProcessAuthor(String author, String publisher) {
        if (author == null || author.isBlank()) return null;
        String a = author;
        // 이메일 제거
        a = a.replaceAll("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}", "");
        // 괄호 내 부가 텍스트 제거 (브랜드/부서 표기 등)
        a = a.replaceAll("\\(.*?\\)", "");
        // 브랜드 토큰 제거
        a = a.replace("매일경제", "").replace("매경닷컴", "").replace("매경", "")
             .replace("한국경제", "").replace("한경", "")
             .replace("조선일보", "").replace("조선비즈", "");
        // 불필요한 구분자 제거 및 트림
        a = a.replaceAll("[|:/]", " ").replaceAll("\\s+", " ").trim();
        // '홍길동 기자' 형태 정규화
        Matcher m = Pattern.compile("([가-힣]{2,4})\\s*(기자)?").matcher(a);
        if (m.find()) {
            a = m.group(1) + " 기자";
        }
        // 매일경제의 경우 '기자' 접미어가 없으면 붙임
        if ("매일경제".equals(publisher) && !a.endsWith("기자")) {
            // 한글 이름만 추출해서 기자 붙이기
            Matcher m2 = Pattern.compile("([가-힣]{2,4})").matcher(a);
            if (m2.find()) {
                a = m2.group(1) + " 기자";
            }
        }
        // 브랜드명만 남았으면 무효
        if (isBrandName(a) || a.equalsIgnoreCase(publisher)) return null;
        return a;
    }
}
