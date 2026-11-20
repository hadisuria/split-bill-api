package io.github.hadsdev.splitbill.config;

import io.github.hadsdev.splitbill.dto.AddExpenseRequest;
import io.github.hadsdev.splitbill.dto.BillGroupResponse;
import io.github.hadsdev.splitbill.dto.CreateBillGroupRequest;
import io.github.hadsdev.splitbill.model.SplitType;
import io.github.hadsdev.splitbill.repository.BillGroupRepository;
import io.github.hadsdev.splitbill.service.BillGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed-data.enabled", havingValue = "true", matchIfMissing = true)
public class SeedDataConfig {

    private final BillGroupRepository billGroupRepository;
    private final BillGroupService billGroupService;

    @Bean
    CommandLineRunner seedDataRunner() {
        return args -> {
            if (billGroupRepository.count() > 0) {
                return;
            }

            BillGroupResponse bandungTrip = billGroupService.createGroup(new CreateBillGroupRequest(
                    "Bandung Weekend",
                    List.of("Hadi", "Bima", "Nadia", "Raka")
            ));

            billGroupService.addExpense(bandungTrip.id(), new AddExpenseRequest(
                    "Toll & Fuel (Cipularang)",
                    new BigDecimal("240000.00"),
                    "Hadi",
                    SplitType.EQUAL,
                    List.of("Hadi", "Bima", "Nadia", "Raka"),
                    null
            ));

            billGroupService.addExpense(bandungTrip.id(), new AddExpenseRequest(
                    "Batagor Kingsley",
                    new BigDecimal("135000.00"),
                    "Bima",
                    SplitType.EQUAL,
                    List.of("Bima", "Nadia"),
                    null
            ));

            billGroupService.addExpense(bandungTrip.id(), new AddExpenseRequest(
                    "Glamping Lembang",
                    new BigDecimal("600000.00"),
                    "Nadia",
                    SplitType.PERCENTAGE,
                    null,
                    Map.of(
                            "Hadi", new BigDecimal("40"),
                            "Bima", new BigDecimal("20"),
                            "Nadia", new BigDecimal("20"),
                            "Raka", new BigDecimal("20")
                    )
            ));

            billGroupService.addExpense(bandungTrip.id(), new AddExpenseRequest(
                    "Kartika Sari Souvenirs",
                    new BigDecimal("225000.00"),
                    "Raka",
                    SplitType.EXACT,
                    null,
                    Map.of(
                            "Hadi", new BigDecimal("75000.00"),
                            "Bima", new BigDecimal("100000.00"),
                            "Nadia", new BigDecimal("50000.00")
                    )
            ));

            BillGroupResponse utilities = billGroupService.createGroup(new CreateBillGroupRequest(
                    "Apartment Utilities",
                    List.of("Dewi", "Ilham", "Tania", "Yoga")
            ));

            billGroupService.addExpense(utilities.id(), new AddExpenseRequest(
                    "PLN Electricity Token",
                    new BigDecimal("250000.00"),
                    "Dewi",
                    SplitType.EQUAL,
                    List.of("Dewi", "Ilham", "Tania"),
                    null
            ));

            billGroupService.addExpense(utilities.id(), new AddExpenseRequest(
                    "Biznet Fiber Internet",
                    new BigDecimal("320000.00"),
                    "Ilham",
                    SplitType.EQUAL,
                    List.of("Dewi", "Ilham", "Tania", "Yoga"),
                    null
            ));
        };
    }
}
