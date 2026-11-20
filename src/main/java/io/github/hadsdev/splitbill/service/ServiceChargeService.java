package io.github.hadsdev.splitbill.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

@Service
public class ServiceChargeService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private final String githubUsername;

    public ServiceChargeService(@Value("${app.github-username}") String githubUsername) {
        if (githubUsername == null || githubUsername.isBlank()) {
            throw new IllegalArgumentException("app.github-username must not be blank");
        }
        this.githubUsername = githubUsername.trim();
    }

    public int calculateServiceChargePct() {
        // Lowercase username ASCII sum mod 10 gives a deterministic 0-9% rate
        int unicodeSum = githubUsername.toLowerCase(Locale.ROOT).chars().sum();
        return unicodeSum % 10;
    }

    public BigDecimal calculateServiceChargeAmount(BigDecimal totalExpenses) {
        if (totalExpenses == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        int pct = calculateServiceChargePct();
        return totalExpenses
                .multiply(BigDecimal.valueOf(pct))
                .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
