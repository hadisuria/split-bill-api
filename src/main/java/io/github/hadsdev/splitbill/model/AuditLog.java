package io.github.hadsdev.splitbill.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String publicId;

    @Column(nullable = false, updatable = false)
    private String groupPublicId;

    @Column(nullable = false, updatable = false)
    private String targetPublicId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private AuditAction action;

    @Column(updatable = false)
    private String actor;

    @Column(nullable = false, updatable = false, length = 500)
    private String details;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public AuditLog(
            String groupPublicId,
            String targetPublicId,
            AuditAction action,
            String actor,
            String details
    ) {
        this.groupPublicId = groupPublicId;
        this.targetPublicId = targetPublicId;
        this.action = action;
        this.actor = actor;
        this.details = details;
    }

    @PrePersist
    void onCreate() {
        if (this.publicId == null) {
            this.publicId = "aud_" + UUID.randomUUID().toString().replace("-", "");
        }
        this.createdAt = Instant.now();
    }
}
