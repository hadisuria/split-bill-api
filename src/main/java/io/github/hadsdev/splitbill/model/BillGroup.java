package io.github.hadsdev.splitbill.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bill_groups")
@Getter
@Setter
@NoArgsConstructor
public class BillGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Non-sequential public ID so internal PKs are never exposed in API URLs
    @Column(nullable = false, unique = true, updatable = false)
    private String publicId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "billGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Participant> participants = new ArrayList<>();

    @OneToMany(mappedBy = "billGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Expense> expenses = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (this.publicId == null) {
            this.publicId = "grp_" + UUID.randomUUID().toString().replace("-", "");
        }
        this.createdAt = Instant.now();
    }

    public void addParticipant(Participant participant) {
        participants.add(participant);
        participant.setBillGroup(this);
    }

    public void addExpense(Expense expense) {
        expenses.add(expense);
        expense.setBillGroup(this);
    }
}
