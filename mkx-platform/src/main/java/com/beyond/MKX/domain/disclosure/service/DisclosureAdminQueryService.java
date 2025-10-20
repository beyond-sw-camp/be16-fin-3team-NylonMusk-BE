package com.beyond.MKX.domain.disclosure.service;

import com.beyond.MKX.domain.disclosure.dto.DisclosureResDto;
import com.beyond.MKX.domain.disclosure.dto.DisclosureTreeResDto;
import com.beyond.MKX.domain.disclosure.entity.DisclosureStatus;
import com.beyond.MKX.domain.disclosure.entity.DisclosureRelationType;
import com.beyond.MKX.domain.disclosure.entity.DisclosureType;
import com.beyond.MKX.domain.disclosure.mapper.DisclosureMapper;
import com.beyond.MKX.domain.disclosure.repository.DisclosureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Comparator;
import java.util.stream.Collectors;
import com.beyond.MKX.domain.disclosure.entity.Disclosure;

@Service
@RequiredArgsConstructor
public class DisclosureAdminQueryService {

    private final DisclosureRepository disclosureRepository;

    public Page<DisclosureResDto> search(
            DisclosureStatus status,
            DisclosureType type,
            UUID stockId,
            String title,
            String displayNo,
            LocalDateTime from,
            LocalDateTime toExclusive,
            Pageable pageable
    ) {
        return disclosureRepository.searchAdmin(status, type, stockId, title, displayNo, from, toExclusive, pageable)
                .map(DisclosureMapper::toRes);
    }

    public List<DisclosureResDto> listRevisionsByDisplayNo(String displayNo) {
        return disclosureRepository.findRevisionsByDisplayNo(displayNo)
                .stream()
                .map(DisclosureMapper::toRes)
                .toList();
    }

    /**
     * 본공시 체인(displayNo=baseNo) + 해당 체인에 연결된 추가공시 체인(및 각 정정)까지 평탄화 목록 반환
     */
    public List<DisclosureResDto> listRelatedByBaseNo(String baseNo) {
        // 1) 본공시 체인(원본+정정) 수집
        List<Disclosure> baseChain =
                disclosureRepository.findRevisionsByDisplayNo(baseNo);

        // 2) 체인 내 ID 목록 → 이 IDs를 previousId로 갖는 ADDITIONAL 찾기
        List<UUID> baseIds = baseChain.stream().map(Disclosure::getId).toList();
        List<Disclosure> additionals = baseIds.isEmpty() ?
                List.of() :
                disclosureRepository.findAdditionalsByPreviousIds(DisclosureRelationType.ADDITIONAL, baseIds);

        // 3) 각 추가공시의 displayNo 체인(정정 포함) 수집
        List<Disclosure> related = new ArrayList<>(baseChain);
        for (Disclosure add : additionals) {
            String addNo = add.getDisplayNo();
            if (addNo != null && !addNo.isBlank()) {
                related.addAll(disclosureRepository.findRevisionsByDisplayNo(addNo));
            } else {
                // 아직 번호 미발급(PENDING 등) → 해당 추가공시 엔티티만 추가
                related.add(add);
            }
        }

        // 4) ID 기준 중복 제거 + 정렬 유지
        Map<UUID, Disclosure> uniq = new LinkedHashMap<>();
        for (Disclosure d : related) uniq.putIfAbsent(d.getId(), d);

        return uniq.values().stream().map(DisclosureMapper::toRes).toList();
    }

    /**
     * 본공시 체인(displayNo=baseNo) + 추가공시 체인들을 트리구조로 반환
     * baseNo가 UUID인 경우 해당 공시부터 역추적하여 원본 공시를 찾음
     */
    public DisclosureTreeResDto getRelatedTreeByBaseNo(String baseNo) {
        // baseNo가 UUID인지 확인
        Disclosure startDisclosure = null;
        String originalDisplayNo = baseNo;
        
        // baseNo가 UUID 형식인지 확인 (8-4-4-4-12 패턴)
        if (baseNo.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")) {
            // UUID인 경우 - 해당 공시부터 역추적하여 원본 공시 찾기
            startDisclosure = disclosureRepository.findById(UUID.fromString(baseNo))
                .orElseThrow(() -> new IllegalArgumentException("공시를 찾을 수 없습니다."));
            originalDisplayNo = findOriginalDisclosure(startDisclosure).getDisplayNo();
        } else {
            // displayNo인 경우 - 그대로 사용
            originalDisplayNo = baseNo;
        }
        
        // 본공시 체인(원본 + 정정 포함)
        List<Disclosure> baseChain = disclosureRepository.findRevisionsByDisplayNo(originalDisplayNo);
        
        // 원본 공시가 체인에 포함되지 않은 경우 추가
        boolean hasOriginal = baseChain.stream().anyMatch(d -> d.getOriginId() == null);
        if (!hasOriginal) {
            // displayNo가 baseNo인 원본 공시 찾기 (originId가 null인 것)
            List<Disclosure> originalDisclosures = disclosureRepository.findByDisplayNoAndOriginIdIsNull(baseNo);
            baseChain.addAll(originalDisclosures);
        }
        
        // revisionNo 기준으로 정렬 (원본이 먼저, 그 다음 정정 순서대로)
        baseChain.sort((a, b) -> {
            Integer revA = a.getRevisionNo() != null ? a.getRevisionNo() : 0;
            Integer revB = b.getRevisionNo() != null ? b.getRevisionNo() : 0;
            return revA.compareTo(revB);
        });
        
        List<DisclosureResDto> baseChainDto = baseChain.stream().map(DisclosureMapper::toRes).toList();

        // 체인 내 ID 목록 → 추가공시 찾기
        List<UUID> baseIds = baseChain.stream().map(Disclosure::getId).toList();
        List<DisclosureTreeResDto> children = new ArrayList<>();
        if (!baseIds.isEmpty()) {
            List<Disclosure> additionals = disclosureRepository
                    .findAdditionalsByPreviousIds(DisclosureRelationType.ADDITIONAL, baseIds);

            // displayNo 기준 그룹핑 (null 키 처리를 위해 빈 문자열로 대체)
            Map<String, List<Disclosure>> byDisplayNo = additionals.stream()
                    .collect(Collectors.groupingBy(d -> d.getDisplayNo() != null ? d.getDisplayNo() : ""));

            // displayNo 정렬(빈 문자열 last)
            List<String> keys = new ArrayList<>(byDisplayNo.keySet());
            keys.sort(Comparator.comparing(s -> s.isEmpty() ? "zzz" : s));

            for (String addNo : keys) {
                List<Disclosure> group = byDisplayNo.get(addNo);
                UUID prevIdHint = group.get(0).getPreviousId();
                List<DisclosureResDto> childChain;
                if (addNo != null && !addNo.isBlank()) {
                    childChain = disclosureRepository.findRevisionsByDisplayNo(addNo)
                            .stream().map(DisclosureMapper::toRes).toList();
                    children.add(new DisclosureTreeResDto(
                            addNo,
                            DisclosureRelationType.ADDITIONAL,
                            prevIdHint,
                            childChain,
                            List.of()
                    ));
                } else {
                    // 번호 미발급 그룹: 각 항목을 별도 노드(체인=단건)로 추가(이전 ID 기준 정렬)
                    group.sort(Comparator.comparing(Disclosure::getCreatedAt));
                    for (Disclosure add : group) {
                        childChain = List.of(DisclosureMapper.toRes(add));
                        children.add(new DisclosureTreeResDto(
                                null,
                                DisclosureRelationType.ADDITIONAL,
                                add.getPreviousId(),
                                childChain,
                                List.of()
                        ));
                    }
                }
            }
        }

        // 루트 노드: relationType NONE, previousId null
        return new DisclosureTreeResDto(originalDisplayNo, DisclosureRelationType.NONE, null, baseChainDto, children);
    }
    
    /**
     * 주어진 공시부터 previousId와 originId를 역추적하여 원본 공시를 찾음
     */
    private Disclosure findOriginalDisclosure(Disclosure disclosure) {
        Disclosure current = disclosure;
        
        // originId가 null이 될 때까지 역추적 (정정공시 체인만)
        while (current.getOriginId() != null) {
            current = disclosureRepository.findById(current.getOriginId())
                .orElse(current);
        }
        
        // 이제 current는 원본 공시이거나 추가공시
        // 추가공시인 경우 previousId로 이동하여 정정공시 체인으로 돌아가기
        while (current.getPreviousId() != null) {
            Disclosure previous = disclosureRepository.findById(current.getPreviousId())
                .orElse(current);
            
            // previous가 정정공시인 경우, 그 정정공시의 원본을 찾기
            if (previous.getOriginId() != null) {
                Disclosure original = previous;
                while (original.getOriginId() != null) {
                    original = disclosureRepository.findById(original.getOriginId())
                        .orElse(original);
                }
                return original;
            }
            
            current = previous;
        }
        
        return current;
    }
}
