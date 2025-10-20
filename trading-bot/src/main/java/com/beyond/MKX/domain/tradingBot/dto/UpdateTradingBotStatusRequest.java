package com.beyond.MKX.domain.tradingBot.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTradingBotStatusRequest {
    
    private String status;           // START/END
}
