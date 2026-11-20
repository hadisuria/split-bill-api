package io.github.hadsdev.splitbill.dto;

import java.math.BigDecimal;

public record ParticipantBalanceResponse(
        String participant,
        BigDecimal totalPaid,
        BigDecimal totalOwed,
        BigDecimal netBalance
) {
}
