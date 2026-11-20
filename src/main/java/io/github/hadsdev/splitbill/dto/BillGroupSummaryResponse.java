package io.github.hadsdev.splitbill.dto;

import java.time.Instant;
import java.util.List;

public record BillGroupSummaryResponse(
        String id,
        String name,
        Instant createdAt,
        List<String> participants,
        int expenseCount
) {
}
