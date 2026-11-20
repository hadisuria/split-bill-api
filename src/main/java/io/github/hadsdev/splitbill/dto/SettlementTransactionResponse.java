package io.github.hadsdev.splitbill.dto;

import java.math.BigDecimal;

public record SettlementTransactionResponse(
        String from,
        String to,
        BigDecimal amount
) {
}
