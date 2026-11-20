package io.github.hadsdev.splitbill.dto;

import io.github.hadsdev.splitbill.model.AuditAction;

import java.time.Instant;

public record AuditLogResponse(
        String id,
        String groupId,
        String targetId,
        AuditAction action,
        String actor,
        String details,
        Instant createdAt
) {
}
