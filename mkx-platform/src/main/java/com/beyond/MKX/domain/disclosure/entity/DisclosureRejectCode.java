package com.beyond.MKX.domain.disclosure.entity;

/**
 * DisclosureRejectCode
 *
 * 공시 심사 반려 사유의 표준화된 코드 집합.
 * - 운영/통계를 위한 분류 용도이며, 상세 설명은 rejectReason 필드에 저장합니다.
 */
public enum DisclosureRejectCode {
    FORMAT_ERROR,     // 문서 형식 오류(깨짐, 비지원 포맷, 암호화 등)
    MISSING_INFO,     // 필수 기재 항목 누락(제목/요약/첨부/스냅샷 등)
    WRONG_STOCK,      // 종목 불일치(기업/티커/stockId 매칭 오류)
    DUPLICATE,        // 중복 제출(동일 제목/동일 파일 등 판단 기준 충족)
    POLICY_VIOLATION, // 게시 정책 위반(부적절 표현, 보안/개인정보 포함 등)
    OTHER             // 기타(상세 사유는 rejectReason에 기입)
}
