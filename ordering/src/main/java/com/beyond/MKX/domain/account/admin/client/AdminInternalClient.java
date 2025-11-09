package com.beyond.MKX.domain.account.admin.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * 플랫폼(mkx-platform-service)이 제공하는 "관리자 내부 API"를 호출하기 위한 Feign 클라이언트.
 *
 * 사용 목적(주요 시나리오)
 * - 주문(오더링) 서비스에서 "증권사 관리자"가 회원 계좌 상태를 변경하려 할 때,
 *   해당 관리자가 어느 증권사에 소속되어 있는지 확인(소속 증권사 계정만 변경 가능하도록 제한).
 *
 * 연결 대상
 * - 서비스명: mkx-platform-service (Eureka 등록명)
 * - 엔드포인트: GET /api/internal/admins/{adminId}/brokerage-id
 */
@FeignClient(name = "mkx-platform-service", contextId = "adminInternalClient", url = "${feign.client.url.mkx-platform-service}")
public interface AdminInternalClient {

    /**
     * 관리자(adminId)의 소속 증권사 ID를 조회한다.
     *
     * @param adminId 관리자 UUID (게이트웨이에서 전달받은 X-User-Id)
     * @return 소속 증권사 ID를 담은 응답 레코드(없으면 null일 수 있음)
     */
    @GetMapping("/api/internal/admins/{adminId}/brokerage-id")
    BrokerageIdRes getBrokerageId(@PathVariable("adminId") UUID adminId);

    /** 응답 페이로드(소속 증권사 ID) */
    record BrokerageIdRes(UUID brokerageId) {}
}
