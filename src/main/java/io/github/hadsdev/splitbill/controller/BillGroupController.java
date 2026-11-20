package io.github.hadsdev.splitbill.controller;

import io.github.hadsdev.splitbill.dto.AddExpenseRequest;
import io.github.hadsdev.splitbill.dto.AuditLogResponse;
import io.github.hadsdev.splitbill.dto.BillGroupResponse;
import io.github.hadsdev.splitbill.dto.BillGroupSummaryResponse;
import io.github.hadsdev.splitbill.dto.CreateBillGroupRequest;
import io.github.hadsdev.splitbill.dto.ExpenseResponse;
import io.github.hadsdev.splitbill.dto.SettlementResponse;
import io.github.hadsdev.splitbill.service.BillGroupService;
import io.github.hadsdev.splitbill.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
@Tag(name = "Bill Groups", description = "Manage bill groups, record shared expenses, and settle balances")
public class BillGroupController {

    private final BillGroupService billGroupService;
    private final SettlementService settlementService;

    @GetMapping
    @Operation(summary = "List bill groups", description = "Returns all bill groups with their public IDs and participant counts.")
    public ResponseEntity<List<BillGroupSummaryResponse>> listGroups() {
        return ResponseEntity.ok(billGroupService.listGroups());
    }

    @PostMapping
    @Operation(summary = "Create a bill group", description = "Starts a new bill group with a name and a unique list of participants.")
    public ResponseEntity<BillGroupResponse> createGroup(@Valid @RequestBody CreateBillGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(billGroupService.createGroup(request));
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "Get bill group details", description = "Fetches a bill group along with its participants and recorded expenses.")
    public ResponseEntity<BillGroupResponse> getGroup(@PathVariable String groupId) {
        return ResponseEntity.ok(billGroupService.getGroup(groupId));
    }

    @PostMapping("/{groupId}/expenses")
    @Operation(summary = "Add an expense", description = "Records a group expense and splits it using EQUAL, EXACT, or PERCENTAGE shares.")
    public ResponseEntity<ExpenseResponse> addExpense(
            @PathVariable String groupId,
            @Valid @RequestBody AddExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(billGroupService.addExpense(groupId, request));
    }

    @GetMapping("/{groupId}/settlement")
    @Operation(summary = "Calculate settlement", description = "Computes net balances, minimized transfers, and the service charge for a group.")
    public ResponseEntity<SettlementResponse> getSettlement(@PathVariable String groupId) {
        return ResponseEntity.ok(settlementService.calculateSettlement(groupId));
    }

    // ! Dev/Preview only: not exposed on public APIs in production
    @GetMapping("/{groupId}/audit-logs")
    @Operation(summary = "List group audit logs", description = "Returns the append-only event log for a group (preview-only; not exposed on public production APIs).")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogs(@PathVariable String groupId) {
        return ResponseEntity.ok(billGroupService.getAuditLogs(groupId));
    }
}
