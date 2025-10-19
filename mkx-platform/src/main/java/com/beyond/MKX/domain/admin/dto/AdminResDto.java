package com.beyond.MKX.domain.admin.dto;

import com.beyond.MKX.domain.admin.entity.Admin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminResDto {
    private UUID id;
    private String email;
    private String name;
    private String phone;
    private String role;
    private String accountNumber; // 선택: 소속 조직의 계좌번호
    private String accountStatus; // 선택: 계좌 상태(또는 account_list 상태)

    // 확장: 조직/상장정보
    private CorporationBrief corporation;      // role=CORPORATION일 때
    private SecuritiesFirmBrief securitiesFirm; // role=BROKERAGE일 때
    private List<StockBrief> stocks;           // 보유 종목 전체
    private List<StockBrief> listedStocks;     // 상장 종목만

    public static AdminResDto from(Admin admin) {
        return AdminResDto.builder()
                .id(admin.getId())
                .email(admin.getEmail())
                .name(admin.getName())
                .phone(admin.getPhone())
                .role(admin.getRole().name())
                .build();
    }

    @Data @AllArgsConstructor @NoArgsConstructor @Builder
    public static class CorporationBrief {
        private UUID id; private String name;
    }
    @Data @AllArgsConstructor @NoArgsConstructor @Builder
    public static class SecuritiesFirmBrief {
        private UUID id; private String name;
    }
    @Data @AllArgsConstructor @NoArgsConstructor @Builder
    public static class StockBrief {
        private UUID id; private String nameKo; private String ticker; private String status;
    }
}
