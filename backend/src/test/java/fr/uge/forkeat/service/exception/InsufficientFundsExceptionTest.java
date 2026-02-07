package fr.uge.forkeat.service.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InsufficientFundsExceptionTest {

    @Test
    void exception_ShouldContainAvailableAndRequiredAmounts() {
        // Given
        Long available = 500L;
        Long required = 1000L;

        // When
        var exception = new InsufficientFundsException(available, required);

        // Then
        assertEquals(available, exception.getAvailable());
        assertEquals(required, exception.getRequired());
    }

    @Test
    void exception_ShouldHaveCorrectMessage() {
        // Given
        Long available = 500L;
        Long required = 1000L;

        // When
        var exception = new InsufficientFundsException(available, required);

        // Then
        assertEquals("Insufficient funds: available 500, required 1000", exception.getMessage());
    }

    @Test
    void exception_ShouldExtendDomainException() {
        // Given/When
        var exception = new InsufficientFundsException(100L, 200L);

        // Then
        assertInstanceOf(DomainException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void exception_ShouldHandleZeroAvailable() {
        // Given/When
        var exception = new InsufficientFundsException(0L, 100L);

        // Then
        assertEquals(0L, exception.getAvailable());
        assertEquals(100L, exception.getRequired());
        assertTrue(exception.getMessage().contains("available 0"));
    }

    @Test
    void exception_ShouldHandleLargeAmounts() {
        // Given
        Long available = Long.MAX_VALUE - 1;
        Long required = Long.MAX_VALUE;

        // When
        var exception = new InsufficientFundsException(available, required);

        // Then
        assertEquals(available, exception.getAvailable());
        assertEquals(required, exception.getRequired());
    }
}
