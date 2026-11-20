package io.github.hadsdev.splitbill.repository;

import io.github.hadsdev.splitbill.model.BillGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillGroupRepository extends JpaRepository<BillGroup, Long> {

    Optional<BillGroup> findByPublicId(String publicId);
}
