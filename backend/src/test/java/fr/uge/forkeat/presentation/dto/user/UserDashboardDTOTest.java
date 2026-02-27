package fr.uge.forkeat.presentation.dto.user;

import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.model.user.projection.UserAccountDetails;
import fr.uge.forkeat.service.model.user.projection.UserSocialStats;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserDashboardDTOTest {

    private UserAccountDetails buildDetails(String username) {
        var user = new User(UUID.randomUUID(), username, "Jean", "Dupont", "jean@example.com",
                UserRole.MEMBER, UserStatus.ACTIVE, AuthMode.LOCAL, null, null, true);
        var socialStats = new UserSocialStats(10L, 5L, 20L, 3L);
        return new UserAccountDetails(user, socialStats, 1500L, 7L);
    }

    @Test
    void from_shouldMapAllFields() {
        var details = buildDetails("chef_test");

        var dto = UserDashboardDTO.from(details);

        assertNotNull(dto);
        assertEquals("chef_test", dto.user().username());
        assertEquals("Jean", dto.user().firstName());
        assertEquals("Dupont", dto.user().lastName());
        assertEquals("jean@example.com", dto.user().email());
        assertEquals(10L, dto.followerCount());
        assertEquals(5L, dto.followingCount());
        assertEquals(20L, dto.totalLikeCount());
        assertEquals(3L, dto.totalSuperLikeCount());
        assertEquals(1500L, dto.walletBalance());
        assertEquals(7L, dto.recipeCount());
    }

    @Test
    void from_shouldThrowWhenNull() {
        assertThrows(NullPointerException.class, () -> UserDashboardDTO.from(null));
    }
}
