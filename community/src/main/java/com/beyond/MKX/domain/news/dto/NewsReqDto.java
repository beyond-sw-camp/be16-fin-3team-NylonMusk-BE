package com.beyond.MKX.domain.news.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsReqDto {
    private String title;
    private String sourceUrl;
    private String sourceName;
    private LocalDateTime publishedAt;
}
