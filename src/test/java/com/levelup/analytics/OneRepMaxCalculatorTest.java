package com.levelup.analytics;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OneRepMaxCalculatorTest {
    private final OneRepMaxCalculator calculator = new OneRepMaxCalculator();
    @Test void calculatesEpleyOneRepMax() {
        assertEquals(new BigDecimal("107.67"), calculator.calculate(new BigDecimal("85"), 8));
    }
    @Test void rejectsInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(BigDecimal.ONE, 0));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(new BigDecimal("-1"), 1));
    }
}
