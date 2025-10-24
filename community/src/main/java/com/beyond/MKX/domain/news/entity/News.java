package com.beyond.MKX.domain.news.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "news")
public class News extends BaseIdAndTimeEntity {

    @Comment("뉴스 제목")
    private String title;

    @Comment("기사 원문 링크")
    private String sourceUrl;

    @Comment("출처명")
    private String sourceName;

    @Comment("발행 시각")
    private LocalDateTime publishedAt;

}
