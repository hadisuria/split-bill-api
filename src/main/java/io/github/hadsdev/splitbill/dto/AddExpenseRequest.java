package io.github.hadsdev.splitbill.dto;

import io.github.hadsdev.splitbill.model.SplitType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AddExpenseRequest(
        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        BigDecimal amount,

        @NotBlank(message = "paidBy is required")
        String paidBy,

        SplitType splitType,

        List<@NotBlank(message = "Participant name must not be blank") String> splitAmong,

        Map<@NotBlank(message = "Participant name must not be blank") String,
                @NotNull(message = "Share value is required")
                @DecimalMin(value = "0.01", message = "Share value must be greater than 0") BigDecimal> shares
) {
}
