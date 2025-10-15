package com.beyond.MKX.domain.stock.util;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.CRC32;

public final class StockTickerGenerator {
    /**
     * 국내 주식 종목코드(Ticker) 생성 유틸
     *
     * - 형식: 6자리 숫자 문자열 (예: "005930")
     * - 충돌 가능성을 전제로 호출부에서 중복검사 후 재시도(attempt) 권장
     * - 시드: corporationId CRC32 + 난수 + attempt
     *
     * 보안/암호학적 유일성은 보장하지 않습니다. 운영에서는 생성 후 유니크 제약/중복검사로 안전장치 필요.
     */

    private StockTickerGenerator() {}

    /** 6자리 0패딩 숫자 문자열 */
    private static String pad6(long n) {
        long v = Math.floorMod(n, 1_000_000L); // 0 ~ 999,999
        return String.format("%06d", v);
    }

    /** UUID 문자열 CRC32 하위 20비트 사용(단순 분산용) */
    private static long crcSeed(UUID id) {
        CRC32 crc = new CRC32();
        crc.update(id.toString().getBytes());
        return crc.getValue() & 0xFFFFF; // 20 bits (0 ~ 1,048,575)
    }

    /**
     * 종목코드 생성
     * - corporationId: 발행사 기준 시드
     * - attempt: 중복 발생 시 호출 측에서 0,1,2... 증가시키며 재시도
     *
     * 반환 예: "012345"
     */
    public static String generate(UUID corporationId, int attempt) {
        long seed = crcSeed(corporationId);
        long rand = ThreadLocalRandom.current().nextLong(1_000_000L); // 0 ~ 999,999
        long mixed = seed + rand + attempt;

        // "000000" 같은 특수값 회피(희소하지만 방어)
        String candidate = pad6(mixed);
        if ("000000".equals(candidate)) {
            candidate = pad6(mixed + 1);
        }
        return candidate;
    }
}
