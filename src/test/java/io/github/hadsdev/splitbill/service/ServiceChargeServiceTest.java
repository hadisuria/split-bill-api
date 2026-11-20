package io.github.hadsdev.splitbill.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceChargeServiceTest {

    @Test
    void shouldComputeZeroPercentForJohndoe47() {
        ServiceChargeService service = new ServiceChargeService("johndoe47");
        assertEquals(0, service.calculateServiceChargePct());
        assertEquals(new BigDecimal("0.00"), service.calculateServiceChargeAmount(new BigDecimal("500000.00")));
    }

    @Test
    void shouldComputeCaseInsensitivePctAndAmountForHadisuria() {
        // "hadisuria" -> 104+97+100+105+115+117+114+105+97 = 954 -> 954 % 10 = 4
        ServiceChargeService lower = new ServiceChargeService("hadisuria");
        ServiceChargeService upper = new ServiceChargeService("HADISURIA");

        assertEquals(4, lower.calculateServiceChargePct());
        assertEquals(4, upper.calculateServiceChargePct());
        assertEquals(new BigDecimal("40000.00"), lower.calculateServiceChargeAmount(new BigDecimal("1000000.00")));
    }

    @Test
    void shouldRejectBlankGithubUsername() {
        assertThrows(IllegalArgumentException.class, () -> new ServiceChargeService("   "));
    }
}
