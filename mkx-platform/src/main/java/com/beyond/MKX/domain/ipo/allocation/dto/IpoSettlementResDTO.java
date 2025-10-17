package com.beyond.MKX.domain.ipo.allocation.dto;

import com.beyond.MKX.domain.ipo.subscription.entity.InvestorType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpoSettlementResDTO {
    private String subscriptionId;
    private String offeringId;
    private InvestorType investorType;

    private String brokerageDepositAccountNumber;
    private String exchangeAccountNumber;

    private BigInteger finalAmt;
    private BigInteger depositAmt;
    private BigInteger moveFromBrokerageToExchange;
    private BigInteger additional;
    private BigInteger refund;

    private String result; // ADDITIONAL | REFUND | EVEN

    public static String decideResult(BigInteger additional, BigInteger refund) {
        if (additional.signum() > 0) return "ADDITIONAL";
        if (refund.signum() > 0) return "REFUND";
        return "EVEN";
    }

    public static IpoSettlementResDTO of(
            String subscriptionId,
            String offeringId,
            InvestorType investorType,
            String brokerageDepositNo,
            String exchangeAccountNo,
            BigInteger finalAmt,
            BigInteger depositAmt,
            BigInteger moveFromBrokerageToExchange,
            BigInteger additional,
            BigInteger refund) {

        return IpoSettlementResDTO.builder()
                .subscriptionId(subscriptionId)
                .offeringId(offeringId)
                .investorType(investorType)
                .brokerageDepositAccountNumber(brokerageDepositNo)
                .exchangeAccountNumber(exchangeAccountNo)
                .finalAmt(finalAmt)
                .depositAmt(depositAmt)
                .moveFromBrokerageToExchange(moveFromBrokerageToExchange)
                .additional(additional)
                .refund(refund)
                .result(decideResult(additional, refund))
                .build();
    }
}
