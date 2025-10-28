package com.beyond.MKX.domain.indicator.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 보조지표 타입 정의
 * 
 * 상단지표: 메인 차트에 오버레이로 표시
 * 하단지표: 별도 차트 영역에 표시
 */
@Getter
@RequiredArgsConstructor
public enum IndicatorType {
    
    // ===== 상단 지표 (Overlay Indicators) =====
    MA("이동평균선", IndicatorPosition.OVERLAY, true),
    ICHIMOKU("일목균형표", IndicatorPosition.OVERLAY, true),
    BOLLINGER_BANDS("볼린저밴드", IndicatorPosition.OVERLAY, true),
    VOLUME_PROFILE("매물대분석", IndicatorPosition.OVERLAY, true),
    ENVELOPE("엔벨로프", IndicatorPosition.OVERLAY, true),
    EMA("지수이동평균선", IndicatorPosition.OVERLAY, true),
    PARABOLIC_SAR("파라볼릭 SAR", IndicatorPosition.OVERLAY, true),
    PRICE_CHANNEL("프라이스 채널", IndicatorPosition.OVERLAY, true),
    
    // ===== 하단 지표 (Separate Panel Indicators) =====
    VOLUME("거래량", IndicatorPosition.BOTTOM, true),
    MASS_INDEX("매스 인덱스", IndicatorPosition.BOTTOM, false),
    MOMENTUM("모멘텀", IndicatorPosition.BOTTOM, false),
    VOLUME_OSCILLATOR("볼륨 오실레이터", IndicatorPosition.BOTTOM, false),
    BOLLINGER_B("볼린저밴드 %B", IndicatorPosition.BOTTOM, false),
    BOLLINGER_WIDTH("볼린저 밴드폭", IndicatorPosition.BOTTOM, false),
    STOCHASTIC("스토캐스틱", IndicatorPosition.BOTTOM, false),
    WILLIAMS_R("윌리엄스 R", IndicatorPosition.BOTTOM, false),
    INTRADAY_INTENSITY("일중 강도 지수", IndicatorPosition.BOTTOM, false),
    CHAIKIN_OSCILLATOR("체이킨 오실레이터", IndicatorPosition.BOTTOM, false),
    TRIX("트릭스", IndicatorPosition.BOTTOM, false),
    PRICE_OSCILLATOR("프라이스 오실레이터", IndicatorPosition.BOTTOM, false),
    AD_LINE("AD라인", IndicatorPosition.BOTTOM, false),
    ADX("ADX", IndicatorPosition.BOTTOM, false),
    ATR("ATR", IndicatorPosition.BOTTOM, false),
    CCI("CCI", IndicatorPosition.BOTTOM, false),
    DMI("DMI", IndicatorPosition.BOTTOM, false),
    MACD("MACD", IndicatorPosition.BOTTOM, false),
    MFI("MFI", IndicatorPosition.BOTTOM, false),
    OBV("OBV", IndicatorPosition.BOTTOM, false),
    ROC("ROC", IndicatorPosition.BOTTOM, false),
    RSI("RSI", IndicatorPosition.BOTTOM, false);
    
    private final String displayName;
    private final IndicatorPosition position;
    private final boolean defaultEnabled;
    
    /**
     * 지표 위치
     */
    public enum IndicatorPosition {
        OVERLAY,    // 메인 차트 위에 오버레이
        BOTTOM      // 하단 별도 패널
    }
}
