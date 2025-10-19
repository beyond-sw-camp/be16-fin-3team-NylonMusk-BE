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
public class DisclosureAdditionalReqFormDto {

    @NotBlank
    private String title;

    private String summary;

    @NotNull
    private MultipartFile file;
}

