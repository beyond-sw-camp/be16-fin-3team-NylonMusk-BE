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
        Disclosure originalDisclosure = null;
        
        // baseNo가 UUID 형식인지 확인 (8-4-4-4-12 패턴)
        if (baseNo.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")) {
            // UUID인 경우 - 해당 공시부터 역추적하여 원본 공시 찾기
            startDisclosure = disclosureRepository.findById(UUID.fromString(baseNo))
                .orElseThrow(() -> new IllegalArgumentException("공시를 찾을 수 없습니다."));
            originalDisclosure = findOriginalDisclosure(startDisclosure);
            originalDisplayNo = originalDisclosure.getDisplayNo();
            
            // displayNo가 null인 경우 (아직 승인되지 않은 공시), 원본 공시의 ID를 사용
            if (originalDisplayNo == null || originalDisplayNo.isBlank()) {
                // 원본 공시 ID를 직접 사용하여 체인 조회
                UUID originalId = originalDisclosure.getId();
                List<Disclosure> chain = new ArrayList<>();
                chain.add(originalDisclosure);
                
                // originId가 originalId인 정정들을 찾기
                List<Disclosure> revisions = disclosureRepository.findAll().stream()
                        .filter(d -> originalId.equals(d.getOriginId()))
                        .filter(d -> d.getRelationType() != DisclosureRelationType.ADDITIONAL)
                        .collect(Collectors.toList());
                chain.addAll(revisions);
                
                // 정렬
                chain.sort((a, b) -> {
                    Integer revA = a.getRevisionNo() != null ? a.getRevisionNo() : 0;
                    Integer revB = b.getRevisionNo() != null ? b.getRevisionNo() : 0;
                    return revA.compareTo(revB);
                });
                
                List<DisclosureResDto> baseChainDto = chain.stream().map(DisclosureMapper::toRes).toList();
                
                // 추가공시 찾기 (기존 로직 재사용)
                List<UUID> baseIds = chain.stream().map(Disclosure::getId).toList();
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
                        
                        // 추가공시의 원본 ID 찾기 (relationType이 ADDITIONAL이고 originId가 null인 것)
                        UUID additionalOriginalId = group.stream()
                                .filter(d -> d.getRelationType() == DisclosureRelationType.ADDITIONAL && d.getOriginId() == null)
                                .map(Disclosure::getId)
                                .findFirst()
                                .orElse(null);
                        
                        List<DisclosureResDto> childChain;
                        if (addNo != null && !addNo.isBlank()) {
                            // 추가공시 체인 가져오기 (추가공시의 정정만 포함)
                            if (additionalOriginalId != null) {
                                // originId가 추가공시 원본 ID인 정정만 포함
                                childChain = disclosureRepository.findRevisionsByDisplayNo(addNo).stream()
                                        .filter(d -> {
                                            // 추가공시 원본이거나, originId가 추가공시 원본 ID인 정정만 포함
                                            return d.getOriginId() == null || d.getOriginId().equals(additionalOriginalId);
                                        })
                                        .map(DisclosureMapper::toRes)
                                        .collect(Collectors.toList());
                            } else {
                                // 추가공시 원본 ID를 찾을 수 없는 경우 (이론적으로 발생하지 않아야 함)
                                childChain = disclosureRepository.findRevisionsByDisplayNo(addNo)
                                        .stream().map(DisclosureMapper::toRes).toList();
                            }
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
                
                return new DisclosureTreeResDto(null, DisclosureRelationType.NONE, null, baseChainDto, children);
            }
        } else {
            // displayNo인 경우
            // 먼저 해당 displayNo로 공시를 찾아봅니다
            List<Disclosure> disclosuresByDisplayNo = disclosureRepository.findByDisplayNo(originalDisplayNo);
            
            // displayNo로 찾은 공시가 추가공시인 경우 역추적 필요
            Disclosure additionalDisclosure = disclosuresByDisplayNo.stream()
                    .filter(d -> d.getRelationType() == DisclosureRelationType.ADDITIONAL)
                    .findFirst()
                    .orElse(null);
            
            if (additionalDisclosure != null) {
                // 추가공시인 경우 역추적하여 본공시 찾기
                originalDisclosure = findOriginalDisclosure(additionalDisclosure);
                originalDisplayNo = originalDisclosure.getDisplayNo();
                
                // 역추적 후에도 displayNo가 null이면 UUID 기반 로직 사용
                if (originalDisplayNo == null || originalDisplayNo.isBlank()) {
                    UUID originalId = originalDisclosure.getId();
                    List<Disclosure> chain = new ArrayList<>();
                    chain.add(originalDisclosure);
                    
                    List<Disclosure> revisions = disclosureRepository.findAll().stream()
                            .filter(d -> originalId.equals(d.getOriginId()))
                            .filter(d -> d.getRelationType() != DisclosureRelationType.ADDITIONAL)
                            .collect(Collectors.toList());
                    chain.addAll(revisions);
                    
                    chain.sort((a, b) -> {
                        Integer revA = a.getRevisionNo() != null ? a.getRevisionNo() : 0;
                        Integer revB = b.getRevisionNo() != null ? b.getRevisionNo() : 0;
                        return revA.compareTo(revB);
                    });
                    
                    List<DisclosureResDto> baseChainDto = chain.stream().map(DisclosureMapper::toRes).toList();
                    
                    // 추가공시 찾기
                    List<UUID> baseIds = chain.stream().map(Disclosure::getId).toList();
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
                            
                            // 추가공시의 원본 ID 찾기 (relationType이 ADDITIONAL이고 originId가 null인 것)
                            UUID additionalOriginalId = group.stream()
                                    .filter(d -> d.getRelationType() == DisclosureRelationType.ADDITIONAL && d.getOriginId() == null)
                                    .map(Disclosure::getId)
                                    .findFirst()
                                    .orElse(null);
                            
                            List<DisclosureResDto> childChain;
                            if (addNo != null && !addNo.isBlank()) {
                                // 추가공시 체인 가져오기 (추가공시의 정정만 포함)
                                if (additionalOriginalId != null) {
                                    // originId가 추가공시 원본 ID인 정정만 포함
                                    childChain = disclosureRepository.findRevisionsByDisplayNo(addNo).stream()
                                            .filter(d -> {
                                                // 추가공시 원본이거나, originId가 추가공시 원본 ID인 정정만 포함
                                                return d.getOriginId() == null || d.getOriginId().equals(additionalOriginalId);
                                            })
                                            .map(DisclosureMapper::toRes)
                                            .collect(Collectors.toList());
                                } else {
                                    // 추가공시 원본 ID를 찾을 수 없는 경우 (이론적으로 발생하지 않아야 함)
                                    childChain = disclosureRepository.findRevisionsByDisplayNo(addNo)
                                            .stream().map(DisclosureMapper::toRes).toList();
                                }
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
                    
                    return new DisclosureTreeResDto(null, DisclosureRelationType.NONE, null, baseChainDto, children);
                }
                // 추가공시를 역추적했고 displayNo가 있는 경우 그대로 사용
                // originalDisplayNo는 이미 209번 줄에서 설정됨
            } else {
                // displayNo이지만 추가공시가 아닌 경우 (본공시) - displayNo 그대로 사용
                originalDisplayNo = baseNo;
            }
        }
        
        // 본공시 체인(원본 + 정정 포함)
        // 먼저 원본 공시를 찾아서 originId를 확인 (본공시만, relationType이 NONE인 것만)
        List<Disclosure> originalDisclosures = disclosureRepository.findByDisplayNoAndOriginIdIsNull(originalDisplayNo).stream()
                .filter(d -> d.getRelationType() == DisclosureRelationType.NONE)
                .collect(Collectors.toList());
        if (originalDisclosures.isEmpty()) {
            // 원본 공시가 없으면 빈 리스트 반환
            return new DisclosureTreeResDto(originalDisplayNo, DisclosureRelationType.NONE, null, List.of(), List.of());
        }
        
        // 원본 공시의 ID를 기준으로 본공시 체인만 필터링
        UUID originalId = originalDisclosures.get(0).getId();
        List<Disclosure> baseChain = disclosureRepository.findRevisionsByDisplayNo(originalDisplayNo).stream()
                .filter(d -> {
                    // 본공시만 포함 (relationType이 NONE이거나 REVISION이면서 originId가 원본 공시 ID인 정정)
                    // 추가공시(relationType이 ADDITIONAL)는 제외
                    if (d.getRelationType() == DisclosureRelationType.ADDITIONAL) {
                        return false;
                    }
                    // 원본 공시이거나, originId가 원본 공시 ID인 정정만 포함
                    // (추가공시의 정정은 originId가 추가공시 ID이므로 제외됨)
                    return d.getOriginId() == null || d.getOriginId().equals(originalId);
                })
                .collect(Collectors.toList());
        
        // 원본 공시가 체인에 포함되지 않은 경우 추가
        boolean hasOriginal = baseChain.stream().anyMatch(d -> d.getOriginId() == null);
        if (!hasOriginal) {
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
                
                // 추가공시의 원본 ID 찾기 (relationType이 ADDITIONAL이고 originId가 null인 것)
                UUID additionalOriginalId = group.stream()
                        .filter(d -> d.getRelationType() == DisclosureRelationType.ADDITIONAL && d.getOriginId() == null)
                        .map(Disclosure::getId)
                        .findFirst()
                        .orElse(null);
                
                List<DisclosureResDto> childChain;
                if (addNo != null && !addNo.isBlank()) {
                    // 추가공시 체인 가져오기 (추가공시의 정정만 포함)
                    if (additionalOriginalId != null) {
                        // originId가 추가공시 원본 ID인 정정만 포함
                        childChain = disclosureRepository.findRevisionsByDisplayNo(addNo).stream()
                                .filter(d -> {
                                    // 추가공시 원본이거나, originId가 추가공시 원본 ID인 정정만 포함
                                    return d.getOriginId() == null || d.getOriginId().equals(additionalOriginalId);
                                })
                                .map(DisclosureMapper::toRes)
                                .collect(Collectors.toList());
                    } else {
                        // 추가공시 원본 ID를 찾을 수 없는 경우 (이론적으로 발생하지 않아야 함)
                        childChain = disclosureRepository.findRevisionsByDisplayNo(addNo)
                                .stream().map(DisclosureMapper::toRes).toList();
                    }
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
