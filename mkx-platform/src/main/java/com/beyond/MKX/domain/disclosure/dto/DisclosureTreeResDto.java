package com.beyond.MKX.domain.disclosure.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.beyond.MKX.domain.disclosure.entity.DisclosureRelationType;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DisclosureTreeResDto(
        String displayNo,
        DisclosureRelationType relationType,
        UUID previousId,
        List<DisclosureResDto> chain,
        List<DisclosureTreeResDto> children
) {}
