package com.beyond.MKX.domain.account.accountlist.util;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.CRC32;

/**
 * 계좌번호 생성 유틸
 *
 * 포맷 규약
 * - 형식: NNN-NNNN-NNNNNNNN (하이픈 포함 17자)
 * - 각 구간은 0 패딩 고정 길이이며 숫자만 사용(3+4+8)
 * - 범위 초과 방지를 위해 각 구간은 모듈로 연산으로 캡(3자리: 1000, 4자리: 10000, 8자리: 100_000_000)
 *
 * 유의사항
 * - 본 생성 규칙은 운영 편의용으로, 암호학적 보안이나 절대적 유일성을 보장하지 않습니다.
 * - 충돌 가능성이 있으므로 생성부에서 중복검사/재시도(attempt) 정책을 함께 사용하세요.
 */
public final class AccountNumberGenerator {

    private AccountNumberGenerator() {}

    /**
     * 구간별 값을 고정 길이로 0 패딩하여 포맷팅합니다.
     * 범위 초과 시 모듈로 연산으로 컷팅합니다.
     */
    private static String format(int bank, int issuer, long serial) {
        return String.format("%03d-%04d-%08d", bank % 1000, issuer % 10000, serial % 100_000_000L);
    }

    /**
     * 증권사 예치금 계좌번호 생성
     * - bankPrefix: 701 (도메인 구분 목적의 임의 값)
     * - issuer: brokerageId를 CRC32로 해싱한 하위 4자리(0~9999)
     * - serial: 8자리 난수 기반 + 재시도 시도수(attempt)를 더해 충돌 회피
     *
     * 충돌 처리
     * - 내부적으로 중복 계좌번호가 감지되면 호출 측에서 attempt를 증가시키며 재시도하는 패턴을 권장합니다.
     */
    public static String brokerageDeposit(UUID brokerageId, int attempt) {
        int bank = 701;
        int issuer = (int) crc32Lower4(brokerageId); // 0~65535 → 포맷에서 4자리로 컷팅
        long base = ThreadLocalRandom.current().nextLong(100_000_000L); // 0 ~ 99,999,999
        long serial = (base + attempt) % 100_000_000L;
        return format(bank, issuer, serial);
    }

    /**
     * 거래소 운영 계좌 기본 포맷(고정값)
     * - 기본값: 900-0000-00000001
     * - 환경에 따라 프로퍼티(설정)로 치환하여 사용 가능
     */
    public static String exchangeDefault() {
        return format(900, 0, 1);
    }

    /**
     * UUID 문자열을 CRC32로 해시한 뒤 하위 16비트만 사용
     * - 단순 분산/구분 용도이며 보안 목적이 아닙니다.
     */
    private static long crc32Lower4(UUID id) {
        CRC32 crc = new CRC32();
        crc.update(id.toString().getBytes());
        return crc.getValue() & 0xFFFF; // 하위 16비트 → 최대 65535
    }
}
