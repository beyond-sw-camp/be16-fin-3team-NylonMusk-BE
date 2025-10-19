package com.beyond.MKX.domain.disclosure.entity;

/**
 * 공시 간 관계 유형
 * - NONE       : 독립 공시(기본)
 * - REVISION   : 정정 공시(원본 번호 유지, originId 사용)
 * - ADDITIONAL : 추가 공시(새 번호 부여, previousId로 본공시 연결)
 */
public enum DisclosureRelationType {
    NONE,
    REVISION,
    ADDITIONAL
}

