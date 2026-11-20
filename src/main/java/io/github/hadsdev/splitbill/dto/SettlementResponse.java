package io.github.hadsdev.splitbill.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record SettlementResponse(
        String groupId,
        String groupName,
        BigDecimal totalExpenses,
        @JsonProperty("service_charge_pct") int serviceChargePct,
        @JsonProperty("service_charge_amount") BigDecimal serviceChargeAmount,
        List<ParticipantBalanceResponse> participantBalances,
        List<SettlementTransactionResponse> settlements
) {
}
