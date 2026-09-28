package fr.uge.forkeat.presentation.dto.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserLoginDTOTest {

    @Test
    void shouldCreateWithValidArguments() {
        var dto = new UserLoginDTO("username", "password");
        assertEquals("username", dto.username());
        assertEquals("password", dto.password());
    }

    @Test
    void shouldThrowOnNullUsername() {
        assertThrows(NullPointerException.class, () -> new UserLoginDTO(null, "password"));
    }

    @Test
    void shouldThrowOnNullPassword() {
        assertThrows(NullPointerException.class, () -> new UserLoginDTO("username", null));
    }
}
