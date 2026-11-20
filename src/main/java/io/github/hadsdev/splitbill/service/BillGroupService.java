package io.github.hadsdev.splitbill.service;

import io.github.hadsdev.splitbill.dto.AddExpenseRequest;
import io.github.hadsdev.splitbill.dto.AuditLogResponse;
import io.github.hadsdev.splitbill.dto.BillGroupResponse;
import io.github.hadsdev.splitbill.dto.BillGroupSummaryResponse;
import io.github.hadsdev.splitbill.dto.CreateBillGroupRequest;
import io.github.hadsdev.splitbill.dto.ExpenseResponse;
import io.github.hadsdev.splitbill.dto.ExpenseShareResponse;
import io.github.hadsdev.splitbill.exception.BusinessValidationException;
import io.github.hadsdev.splitbill.exception.ResourceNotFoundException;
import io.github.hadsdev.splitbill.model.AuditAction;
import io.github.hadsdev.splitbill.model.AuditLog;
import io.github.hadsdev.splitbill.model.BillGroup;
import io.github.hadsdev.splitbill.model.Expense;
import io.github.hadsdev.splitbill.model.ExpenseShare;
import io.github.hadsdev.splitbill.model.Participant;
import io.github.hadsdev.splitbill.model.SplitType;
import io.github.hadsdev.splitbill.repository.AuditLogRepository;
import io.github.hadsdev.splitbill.repository.BillGroupRepository;
import io.github.hadsdev.splitbill.repository.ExpenseRepository;
import io.github.hadsdev.splitbill.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BillGroupService {

    private static final BigDecimal ONE_CENT = new BigDecimal("0.01");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final BillGroupRepository billGroupRepository;
    private final ParticipantRepository participantRepository;
    private final ExpenseRepository expenseRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional
    public BillGroupResponse createGroup(CreateBillGroupRequest request) {
        BillGroup group = new BillGroup();
        group.setName(request.name().trim());

        Set<String> seenNames = new HashSet<>();
        for (String rawName : request.participants()) {
            String trimmed = rawName.trim();
            if (!seenNames.add(trimmed.toLowerCase(Locale.ROOT))) {
                throw new BusinessValidationException("Duplicate participant name: " + trimmed);
            }
            group.addParticipant(new Participant(trimmed));
        }

        BillGroup saved = billGroupRepository.save(group);
        auditLogRepository.save(new AuditLog(
                saved.getPublicId(),
                saved.getPublicId(),
                AuditAction.GROUP_CREATED,
                null,
                "Created group '" + saved.getName() + "' with " + saved.getParticipants().size() + " participants"
        ));
        return toGroupResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BillGroupSummaryResponse> listGroups() {
        return billGroupRepository.findAll().stream()
                .map(group -> new BillGroupSummaryResponse(
                        group.getPublicId(),
                        group.getName(),
                        group.getCreatedAt(),
                        group.getParticipants().stream().map(Participant::getName).toList(),
                        group.getExpenses().size()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public BillGroupResponse getGroup(String publicId) {
        BillGroup group = billGroupRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill group not found: " + publicId));
        return toGroupResponse(group);
    }

    @Transactional
    public ExpenseResponse addExpense(String publicId, AddExpenseRequest request) {
        BillGroup group = billGroupRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill group not found: " + publicId));

        Long internalGroupId = group.getId();
        Participant paidBy = findParticipantInGroup(internalGroupId, publicId, request.paidBy());
        SplitType splitType = request.splitType() != null ? request.splitType() : SplitType.EQUAL;
        BigDecimal totalAmount = request.amount().setScale(2, RoundingMode.HALF_UP);

        Expense expense = new Expense();
        expense.setDescription(request.description().trim());
        expense.setAmount(totalAmount);
        expense.setSplitType(splitType);
        expense.setPaidBy(paidBy);
        expense.setBillGroup(group);

        List<ExpenseShare> computedShares = switch (splitType) {
            case EQUAL -> buildEqualShares(internalGroupId, publicId, totalAmount, request.splitAmong());
            case EXACT -> buildExactShares(internalGroupId, publicId, totalAmount, request.shares());
            case PERCENTAGE -> buildPercentageShares(internalGroupId, publicId, totalAmount, request.shares());
        };

        computedShares.forEach(expense::addShare);
        Expense saved = expenseRepository.save(expense);
        auditLogRepository.save(new AuditLog(
                group.getPublicId(),
                saved.getPublicId(),
                AuditAction.EXPENSE_ADDED,
                paidBy.getName(),
                "Added expense '" + saved.getDescription() + "' of " + saved.getAmount()
                        + " (" + saved.getSplitType() + ") paid by " + paidBy.getName()
        ));
        return toExpenseResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogs(String publicId) {
        if (billGroupRepository.findByPublicId(publicId).isEmpty()) {
            throw new ResourceNotFoundException("Bill group not found: " + publicId);
        }
        return auditLogRepository.findByGroupPublicIdOrderByCreatedAtAscIdAsc(publicId).stream()
                .map(log -> new AuditLogResponse(
                        log.getPublicId(),
                        log.getGroupPublicId(),
                        log.getTargetPublicId(),
                        log.getAction(),
                        log.getActor(),
                        log.getDetails(),
                        log.getCreatedAt()
                ))
                .toList();
    }

    private List<ExpenseShare> buildEqualShares(
            Long internalGroupId,
            String publicGroupId,
            BigDecimal totalAmount,
            List<String> splitAmong
    ) {
        if (splitAmong == null || splitAmong.isEmpty()) {
            throw new BusinessValidationException("splitAmong is required for EQUAL split");
        }

        List<Participant> participants = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();
        for (String name : splitAmong) {
            Participant participant = findParticipantInGroup(internalGroupId, publicGroupId, name);
            if (!seenIds.add(participant.getId())) {
                throw new BusinessValidationException("Duplicate participant in splitAmong: " + participant.getName());
            }
            participants.add(participant);
        }

        BigDecimal count = BigDecimal.valueOf(participants.size());
        BigDecimal baseShare = totalAmount.divide(count, 2, RoundingMode.DOWN);
        BigDecimal remainder = totalAmount.subtract(baseShare.multiply(count));

        return distributeRemainder(participants, baseShare, remainder);
    }

    private List<ExpenseShare> buildExactShares(
            Long internalGroupId,
            String publicGroupId,
            BigDecimal totalAmount,
            Map<String, BigDecimal> rawShares
    ) {
        if (rawShares == null || rawShares.isEmpty()) {
            throw new BusinessValidationException("shares map is required for EXACT split");
        }

        Map<Participant, BigDecimal> resolved = resolveShareMap(internalGroupId, publicGroupId, rawShares);
        BigDecimal sum = BigDecimal.ZERO;
        List<ExpenseShare> shares = new ArrayList<>();

        for (Map.Entry<Participant, BigDecimal> entry : resolved.entrySet()) {
            BigDecimal amount = entry.getValue().setScale(2, RoundingMode.HALF_UP);
            sum = sum.add(amount);
            shares.add(new ExpenseShare(entry.getKey(), amount));
        }

        if (sum.compareTo(totalAmount) != 0) {
            throw new BusinessValidationException(
                    "Exact shares sum (" + sum + ") must equal expense amount (" + totalAmount + ")"
            );
        }

        return shares;
    }

    private List<ExpenseShare> buildPercentageShares(
            Long internalGroupId,
            String publicGroupId,
            BigDecimal totalAmount,
            Map<String, BigDecimal> rawPercentages
    ) {
        if (rawPercentages == null || rawPercentages.isEmpty()) {
            throw new BusinessValidationException("shares map is required for PERCENTAGE split");
        }

        Map<Participant, BigDecimal> resolved = resolveShareMap(internalGroupId, publicGroupId, rawPercentages);
        BigDecimal pctSum = resolved.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (pctSum.compareTo(ONE_HUNDRED) != 0) {
            throw new BusinessValidationException("Percentages must sum to 100, got: " + pctSum);
        }

        List<ExpenseShare> shares = new ArrayList<>();
        BigDecimal allocated = BigDecimal.ZERO;
        for (Map.Entry<Participant, BigDecimal> entry : resolved.entrySet()) {
            BigDecimal share = totalAmount
                    .multiply(entry.getValue())
                    .divide(ONE_HUNDRED, 2, RoundingMode.DOWN);
            allocated = allocated.add(share);
            shares.add(new ExpenseShare(entry.getKey(), share));
        }

        // Distribute leftover cents one by one so shares always sum to totalAmount
        BigDecimal remainder = totalAmount.subtract(allocated);
        int index = 0;
        while (remainder.compareTo(BigDecimal.ZERO) > 0) {
            ExpenseShare share = shares.get(index % shares.size());
            share.setAmount(share.getAmount().add(ONE_CENT));
            remainder = remainder.subtract(ONE_CENT);
            index++;
        }

        return shares;
    }

    private List<ExpenseShare> distributeRemainder(
            List<Participant> participants,
            BigDecimal baseShare,
            BigDecimal remainder
    ) {
        List<ExpenseShare> shares = new ArrayList<>(participants.size());
        BigDecimal remaining = remainder;
        for (Participant participant : participants) {
            BigDecimal amount = baseShare;
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                amount = amount.add(ONE_CENT);
                remaining = remaining.subtract(ONE_CENT);
            }
            shares.add(new ExpenseShare(participant, amount));
        }
        return shares;
    }

    private Map<Participant, BigDecimal> resolveShareMap(
            Long internalGroupId,
            String publicGroupId,
            Map<String, BigDecimal> rawShares
    ) {
        Map<Participant, BigDecimal> resolved = new LinkedHashMap<>();
        Set<Long> seenIds = new HashSet<>();
        for (Map.Entry<String, BigDecimal> entry : rawShares.entrySet()) {
            Participant participant = findParticipantInGroup(internalGroupId, publicGroupId, entry.getKey());
            if (!seenIds.add(participant.getId())) {
                throw new BusinessValidationException("Duplicate participant in shares: " + participant.getName());
            }
            resolved.put(participant, entry.getValue());
        }
        return resolved;
    }

    private Participant findParticipantInGroup(Long internalGroupId, String publicGroupId, String name) {
        return participantRepository.findByBillGroupIdAndNameIgnoreCase(internalGroupId, name.trim())
                .orElseThrow(() -> new BusinessValidationException(
                        "Participant '" + name + "' is not a member of bill group " + publicGroupId
                ));
    }

    private BillGroupResponse toGroupResponse(BillGroup group) {
        List<String> participantNames = group.getParticipants().stream()
                .map(Participant::getName)
                .toList();
        List<ExpenseResponse> expenseResponses = group.getExpenses().stream()
                .map(this::toExpenseResponse)
                .toList();
        return new BillGroupResponse(
                group.getPublicId(),
                group.getName(),
                group.getCreatedAt(),
                participantNames,
                expenseResponses
        );
    }

    private ExpenseResponse toExpenseResponse(Expense expense) {
        List<ExpenseShareResponse> shareResponses = expense.getShares().stream()
                .map(s -> new ExpenseShareResponse(s.getParticipant().getName(), s.getAmount()))
                .toList();
        return new ExpenseResponse(
                expense.getPublicId(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getSplitType(),
                expense.getPaidBy().getName(),
                expense.getCreatedAt(),
                shareResponses
        );
    }
}
