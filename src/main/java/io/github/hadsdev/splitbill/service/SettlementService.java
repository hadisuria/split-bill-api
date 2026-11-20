package io.github.hadsdev.splitbill.service;

import io.github.hadsdev.splitbill.dto.ParticipantBalanceResponse;
import io.github.hadsdev.splitbill.dto.SettlementResponse;
import io.github.hadsdev.splitbill.dto.SettlementTransactionResponse;
import io.github.hadsdev.splitbill.exception.ResourceNotFoundException;
import io.github.hadsdev.splitbill.model.BillGroup;
import io.github.hadsdev.splitbill.model.Expense;
import io.github.hadsdev.splitbill.model.ExpenseShare;
import io.github.hadsdev.splitbill.model.Participant;
import io.github.hadsdev.splitbill.repository.BillGroupRepository;
import io.github.hadsdev.splitbill.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final BillGroupRepository billGroupRepository;
    private final ExpenseRepository expenseRepository;
    private final ServiceChargeService serviceChargeService;

    @Transactional(readOnly = true)
    public SettlementResponse calculateSettlement(String publicGroupId) {
        BillGroup group = billGroupRepository.findByPublicId(publicGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill group not found: " + publicGroupId));

        List<Expense> expenses = expenseRepository.findByBillGroupIdOrderByCreatedAtAsc(group.getId());

        Map<Long, BigDecimal> paidByParticipant = new LinkedHashMap<>();
        Map<Long, BigDecimal> owedByParticipant = new LinkedHashMap<>();
        BigDecimal zero = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (Participant participant : group.getParticipants()) {
            paidByParticipant.put(participant.getId(), zero);
            owedByParticipant.put(participant.getId(), zero);
        }

        BigDecimal totalExpenses = zero;
        for (Expense expense : expenses) {
            totalExpenses = totalExpenses.add(expense.getAmount());
            Long payerId = expense.getPaidBy().getId();
            paidByParticipant.merge(payerId, expense.getAmount(), BigDecimal::add);

            for (ExpenseShare share : expense.getShares()) {
                Long participantId = share.getParticipant().getId();
                owedByParticipant.merge(participantId, share.getAmount(), BigDecimal::add);
            }
        }

        List<ParticipantBalanceResponse> balances = new ArrayList<>();
        PriorityQueue<BalanceEntry> creditors = new PriorityQueue<>(
                Comparator.comparing(BalanceEntry::amount).reversed()
        );
        PriorityQueue<BalanceEntry> debtors = new PriorityQueue<>(
                Comparator.comparing(BalanceEntry::amount).reversed()
        );

        for (Participant participant : group.getParticipants()) {
            BigDecimal paid = paidByParticipant.get(participant.getId());
            BigDecimal owed = owedByParticipant.get(participant.getId());
            BigDecimal net = paid.subtract(owed);

            balances.add(new ParticipantBalanceResponse(participant.getName(), paid, owed, net));

            if (net.compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(new BalanceEntry(participant.getName(), net));
            } else if (net.compareTo(BigDecimal.ZERO) < 0) {
                debtors.add(new BalanceEntry(participant.getName(), net.abs()));
            }
        }

        List<SettlementTransactionResponse> settlements = minimizeTransactions(creditors, debtors);

        int serviceChargePct = serviceChargeService.calculateServiceChargePct();
        BigDecimal serviceChargeAmount = serviceChargeService.calculateServiceChargeAmount(totalExpenses);

        return new SettlementResponse(
                group.getPublicId(),
                group.getName(),
                totalExpenses,
                serviceChargePct,
                serviceChargeAmount,
                balances,
                settlements
        );
    }

    private List<SettlementTransactionResponse> minimizeTransactions(
            PriorityQueue<BalanceEntry> creditors,
            PriorityQueue<BalanceEntry> debtors
    ) {
        // Match largest debtor with largest creditor to minimize total transfers
        List<SettlementTransactionResponse> transactions = new ArrayList<>();

        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            BalanceEntry creditor = creditors.poll();
            BalanceEntry debtor = debtors.poll();

            BigDecimal transfer = creditor.amount().min(debtor.amount());
            transactions.add(new SettlementTransactionResponse(
                    debtor.name(),
                    creditor.name(),
                    transfer
            ));

            BigDecimal remainingCredit = creditor.amount().subtract(transfer);
            BigDecimal remainingDebt = debtor.amount().subtract(transfer);

            if (remainingCredit.compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(new BalanceEntry(creditor.name(), remainingCredit));
            }
            if (remainingDebt.compareTo(BigDecimal.ZERO) > 0) {
                debtors.add(new BalanceEntry(debtor.name(), remainingDebt));
            }
        }

        return transactions;
    }

    private record BalanceEntry(String name, BigDecimal amount) {
    }
}
