package io.github.hadsdev.splitbill.dto;

import io.github.hadsdev.splitbill.model.SplitType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ExpenseResponse(
        String id,
        String description,
        BigDecimal amount,
        SplitType splitType,
        String paidBy,
        Instant createdAt,
        List<ExpenseShareResponse> shares
) {
}
