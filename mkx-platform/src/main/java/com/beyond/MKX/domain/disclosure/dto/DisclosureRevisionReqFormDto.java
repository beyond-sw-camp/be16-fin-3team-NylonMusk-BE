package com.beyond.MKX.domain.disclosure.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class DisclosureRevisionReqFormDto {

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    private String summary;

    @NotNull(message = "정정 파일은 필수입니다.")
    private MultipartFile file;
}
