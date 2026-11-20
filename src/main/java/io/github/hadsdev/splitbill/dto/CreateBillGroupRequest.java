package io.github.hadsdev.splitbill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateBillGroupRequest(
        @NotBlank(message = "Group name is required")
        String name,

        @NotEmpty(message = "At least one participant is required")
        List<@NotBlank(message = "Participant name must not be blank") String> participants
) {
}
