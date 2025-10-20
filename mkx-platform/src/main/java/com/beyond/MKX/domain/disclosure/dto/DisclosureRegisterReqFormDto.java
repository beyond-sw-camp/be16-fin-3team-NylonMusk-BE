package com.beyond.MKX.domain.disclosure.dto;

import com.beyond.MKX.domain.disclosure.entity.DisclosureType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class DisclosureRegisterReqFormDto {

    @NotNull
    private UUID stockId;

    @NotNull
    private DisclosureType disclosureType;

    @NotBlank
    private String title;

    private String summary;

    @NotNull
    private MultipartFile file;

    @NotBlank
    private String stockNameSnapshot;

    @NotBlank
    private String tickerSnapshot;

    // 최근 중복 경고를 무시하고 강행 여부 (기본 false) true 시 등록은 가능
    private Boolean force;
}
