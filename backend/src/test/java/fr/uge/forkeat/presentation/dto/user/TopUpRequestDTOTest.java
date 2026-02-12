package fr.uge.forkeat.presentation.dto.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TopUpRequestDTOTest {

    @Test
    void shouldCreateWithValidAmount() {
        var dto = new TopUpRequestDTO(500L);
        assertEquals(500L, dto.amount());
    }

    @Test
    void shouldCreateWithMinimumAmount() {
        var dto = new TopUpRequestDTO(100L);
        assertEquals(100L, dto.amount());
    }

    @Test
    void shouldThrowWhenAmountBelowMinimum() {
        assertThrows(IllegalArgumentException.class, () -> new TopUpRequestDTO(99L));
    }

    @Test
    void shouldThrowWhenAmountIsZero() {
        assertThrows(IllegalArgumentException.class, () -> new TopUpRequestDTO(0L));
    }

    @Test
    void shouldThrowWhenAmountIsNegative() {
        assertThrows(IllegalArgumentException.class, () -> new TopUpRequestDTO(-100L));
    }
}
