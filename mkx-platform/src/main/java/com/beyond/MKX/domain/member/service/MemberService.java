package com.beyond.MKX.domain.member.service;

import com.beyond.MKX.common.exception.DuplicateResourceException;
import com.beyond.MKX.domain.member.entity.Member;
import com.beyond.MKX.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    /**
     * userId에 해당하는 Member 조회
     * - 없는 경우 null 반환 (Controller에서 처리)
     * - 추후 권한/상태 체크 같은 비즈니스 로직 추가 가능
     */
    public Member getMemberById(UUID userId) {
        return memberRepository.findById(userId).orElseThrow(() ->
                new DuplicateResourceException("회원 정보를 찾을 수 없습니다."));
    }
}