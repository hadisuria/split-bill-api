package io.github.hadsdev.splitbill.repository;

import io.github.hadsdev.splitbill.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByGroupPublicIdOrderByCreatedAtAscIdAsc(String groupPublicId);
}
