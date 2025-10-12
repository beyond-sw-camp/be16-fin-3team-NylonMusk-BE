package com.beyond.MKX.domain.account.member.util;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.CRC32;

/**
 * 회원(일반 유저) 계좌번호 생성기
 *
 * 포맷: {@code 702-IIII-SSSSSSSS}
 *  - 702        : 회원계좌 구분 prefix (필요 시 환경설정으로 분리 가능)
 *  - IIII       : 증권사 식별(브로커리지 UUID의 CRC32 하위 16bit → 4자리 10진)
 *  - SSSSSSSS   : 8자리 일련(난수 기반 + 재시도 시 {@code attempt} 증가로 변화)
 *
 * 설계 의도
 *  - 같은 증권사 내에서만 일련번호 충돌 가능성이 의미를 가지며, 1e8(1억) 공간을 사용해
 *    확률을 충분히 낮춘다.
 *  - 동시성 레이스나 드문 충돌은 서비스 레벨에서 "저장 시도 → UNIQUE 예외 catch → 다음 후보"로 흡수한다.
 */
public final class MemberAccountNumberGenerator {

    private MemberAccountNumberGenerator() {}

    /**
     * 계좌번호 생성
     *
     * @param brokerageId 증권사 UUID(IIII 필드 산출에 사용)
     * @param attempt     재시도 횟수(충돌 시 증가하여 SSSSSSSS 변화 유도)
     * @return 포맷 {@code 702-IIII-SSSSSSSS}의 계좌번호
     */
    public static String generate(UUID brokerageId, int attempt) {
        // 회원 계좌 prefix (필요 시 환경설정으로 분리 가능)
        int bank = 702;

        // 증권사 식별: UUID → CRC32 → 하위 16bit(0xFFFF)
        // 4자리 고정 규약을 지키기 위해 10000으로 캡(초과 자릿수 방지, 5자리 → 4자리)
        int issuer = (int) (crc32(brokerageId) & 0xFFFF);
        int issuer4 = issuer % 10000;

        // 0 <= base < 100,000,000 (8자리 난수)
        long base = ThreadLocalRandom.current().nextLong(100_000_000L);

        // 재시도 시 attempt를 더해 직전 충돌 후보와 달라지도록 하고, 8자리 범위 내에서 모듈러 처리
        long serial = (base + attempt) % 100_000_000L;

        // 3-4-8 자리 고정 포맷(0-padding)
        // bank도 3자리 범위 보장(안전), issuer는 4자리로 캡핑된 값 사용
        return String.format("%03d-%04d-%08d", bank % 1000, issuer4, serial);
    }

    /**
     * UUID 문자열의 CRC32 값을 계산
     * - 플랫폼 간 일관성을 위해 UTF-8 바이트로 변환 후 CRC32 적용
     * 항상 0 이상 2^32-1(약 42억) 사이의 long 값이 반환된다.
     * - 같은 UUID면 항상 같은 숫자, 다른 UUID면 거의 다른 숫자.
     */
    private static long crc32(UUID id) {
        CRC32 crc = new CRC32();
        crc.update(id.toString().getBytes(StandardCharsets.UTF_8));
        return crc.getValue();
    }
}
