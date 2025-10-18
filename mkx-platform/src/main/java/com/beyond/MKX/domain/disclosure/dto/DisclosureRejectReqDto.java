package com.beyond.MKX.domain.disclosure.dto;

import com.beyond.MKX.domain.disclosure.entity.DisclosureRejectCode;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DisclosureRejectReqDto {
    @NotNull
    private DisclosureRejectCode code;

    // OTHER가 아닐 때는 선택, OTHER일 때는 필수
    @Size(max = 255)
    private String reason;

    @AssertTrue(message = "기타(OTHER) 사유 선택 시 reason은 필수입니다.")
    public boolean isReasonValid() {
        if (code == null) return true;
        if (code == DisclosureRejectCode.OTHER) {
            return reason != null && !reason.isBlank();
        }
        return true;
    }
}
