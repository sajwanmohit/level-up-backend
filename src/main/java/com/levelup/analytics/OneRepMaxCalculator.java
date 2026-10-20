package com.levelup.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class OneRepMaxCalculator {
    public BigDecimal calculate(BigDecimal weight, int reps) {
        if (weight.signum() < 0 || reps < 1) throw new IllegalArgumentException("Weight must be non-negative and reps must be positive");
        return weight.multiply(BigDecimal.ONE.add(BigDecimal.valueOf(reps).divide(BigDecimal.valueOf(30), 8, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
