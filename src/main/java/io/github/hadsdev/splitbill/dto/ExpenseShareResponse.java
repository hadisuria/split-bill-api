package io.github.hadsdev.splitbill.dto;

import java.math.BigDecimal;

public record ExpenseShareResponse(
        String participant,
        BigDecimal amount
) {
}
