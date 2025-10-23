package com.beyond.MKX.domain.delisting.entity;

public enum DelistingStage {
    NORMAL,             // 정상 상장
    WARNING,            // 경고
    CAUTION,            // 주의
    DELISTING_NOTICE,   // 상장폐지 예고
    DELISTING_PROCESS,  // 상장폐지 절차 진행 중
    DELISTED            // 상장폐지 완료
}
