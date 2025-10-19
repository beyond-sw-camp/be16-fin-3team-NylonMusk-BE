package com.beyond.MKX.domain.disclosure.dto;

import com.beyond.MKX.domain.disclosure.entity.DisclosureRelationType;

import java.util.List;
import java.util.UUID;

public record DisclosureTreeResDto(
        String displayNo,
        DisclosureRelationType relationType,
        UUID previousId,
        List<DisclosureResDto> chain,
        List<DisclosureTreeResDto> children
) {}
