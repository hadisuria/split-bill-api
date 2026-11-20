package io.github.hadsdev.splitbill.service;

import io.github.hadsdev.splitbill.dto.SettlementResponse;
import io.github.hadsdev.splitbill.dto.SettlementTransactionResponse;
import io.github.hadsdev.splitbill.exception.ResourceNotFoundException;
import io.github.hadsdev.splitbill.model.BillGroup;
import io.github.hadsdev.splitbill.model.Expense;
import io.github.hadsdev.splitbill.model.ExpenseShare;
import io.github.hadsdev.splitbill.model.Participant;
import io.github.hadsdev.splitbill.model.SplitType;
import io.github.hadsdev.splitbill.repository.BillGroupRepository;
import io.github.hadsdev.splitbill.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private BillGroupRepository billGroupRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    private SettlementService settlementService;

    @BeforeEach
    void setUp() {
        // "hadisuria" -> 4% service charge
        ServiceChargeService serviceChargeService = new ServiceChargeService("hadisuria");
        settlementService = new SettlementService(billGroupRepository, expenseRepository, serviceChargeService);
    }

    @Test
    void shouldCalculateMinimizedSettlementAndServiceCharge() {
        String publicId = "grp_bandung123";
        BillGroup group = createGroup(1L, publicId, "Bandung Weekend", "Hadi", "Bima", "Nadia");
        Participant hadi = group.getParticipants().get(0);
        Participant bima = group.getParticipants().get(1);
        Participant nadia = group.getParticipants().get(2);

        Expense toll = new Expense();
        toll.setAmount(new BigDecimal("240000.00"));
        toll.setPaidBy(hadi);
        toll.setSplitType(SplitType.EQUAL);
        toll.addShare(new ExpenseShare(hadi, new BigDecimal("80000.00")));
        toll.addShare(new ExpenseShare(bima, new BigDecimal("80000.00")));
        toll.addShare(new ExpenseShare(nadia, new BigDecimal("80000.00")));

        Expense batagor = new Expense();
        batagor.setAmount(new BigDecimal("120000.00"));
        batagor.setPaidBy(bima);
        batagor.setSplitType(SplitType.EQUAL);
        batagor.addShare(new ExpenseShare(bima, new BigDecimal("60000.00")));
        batagor.addShare(new ExpenseShare(nadia, new BigDecimal("60000.00")));

        when(billGroupRepository.findByPublicId(publicId)).thenReturn(Optional.of(group));
        when(expenseRepository.findByBillGroupIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(toll, batagor));

        SettlementResponse response = settlementService.calculateSettlement(publicId);

        assertEquals(publicId, response.groupId());
        assertEquals(new BigDecimal("360000.00"), response.totalExpenses());
        assertEquals(4, response.serviceChargePct());
        assertEquals(new BigDecimal("14400.00"), response.serviceChargeAmount());
        assertEquals(2, response.settlements().size());

        SettlementTransactionResponse first = response.settlements().get(0);
        assertEquals("Nadia", first.from());
        assertEquals("Hadi", first.to());
        assertEquals(new BigDecimal("140000.00"), first.amount());

        SettlementTransactionResponse second = response.settlements().get(1);
        assertEquals("Bima", second.from());
        assertEquals("Hadi", second.to());
        assertEquals(new BigDecimal("20000.00"), second.amount());
    }

    @Test
    void shouldHandleGroupWithNoExpenses() {
        String publicId = "grp_empty123";
        BillGroup group = createGroup(2L, publicId, "Apartment Utilities", "Dewi", "Ilham");
        when(billGroupRepository.findByPublicId(publicId)).thenReturn(Optional.of(group));
        when(expenseRepository.findByBillGroupIdOrderByCreatedAtAsc(2L)).thenReturn(List.of());

        SettlementResponse response = settlementService.calculateSettlement(publicId);

        assertEquals(new BigDecimal("0.00"), response.totalExpenses());
        assertEquals(4, response.serviceChargePct());
        assertEquals(new BigDecimal("0.00"), response.serviceChargeAmount());
        assertTrue(response.settlements().isEmpty());
    }

    @Test
    void shouldThrowWhenGroupNotFound() {
        when(billGroupRepository.findByPublicId("grp_missing")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> settlementService.calculateSettlement("grp_missing"));
    }

    private BillGroup createGroup(Long id, String publicId, String name, String... participantNames) {
        BillGroup group = new BillGroup();
        group.setId(id);
        group.setPublicId(publicId);
        group.setName(name);
        long pId = 1L;
        for (String pName : participantNames) {
            Participant p = new Participant(pName);
            p.setId(pId++);
            group.addParticipant(p);
        }
        return group;
    }
}
