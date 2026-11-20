package io.github.hadsdev.splitbill.repository;

import io.github.hadsdev.splitbill.model.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    Optional<Participant> findByBillGroupIdAndNameIgnoreCase(Long billGroupId, String name);

    List<Participant> findByBillGroupId(Long billGroupId);
}
