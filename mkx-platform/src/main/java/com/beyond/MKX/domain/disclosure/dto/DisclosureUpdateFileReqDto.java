package com.beyond.MKX.domain.disclosure.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class DisclosureUpdateFileReqDto {
    private MultipartFile file;
    private String summary;
}

