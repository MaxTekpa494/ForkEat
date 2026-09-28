package fr.uge.forkeat.presentation.security;

import fr.uge.forkeat.infrastructure.AbstractIntegrationTest;
import fr.uge.forkeat.service.PromotionService;
import fr.uge.forkeat.service.model.superlike.Promotion;
import fr.uge.forkeat.service.model.superlike.PromotionStatus;
import fr.uge.forkeat.service.port.PromotionSchedulingPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("test")
@Transactional
public class AdminPromotionControllerSecurityTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PromotionService promotionService;

    @MockitoBean
    private PromotionSchedulingPort schedulingService;

    private UUID promotionId;

    @BeforeEach
    void setup() {
        promotionId = UUID.randomUUID();
        var mockPromotion = createPromotion(promotionId);

        lenient().when(promotionService.findAll()).thenReturn(List.of(mockPromotion));
        lenient().when(promotionService.findById(any())).thenReturn(mockPromotion);
        lenient().when(promotionService.create(any(), any(), any(), anyLong(), any())).thenReturn(mockPromotion);
        lenient().when(promotionService.update(any(), any(), any(), any(), any(), any())).thenReturn(mockPromotion);
        lenient().doNothing().when(promotionService).cancel(any());

        lenient().doNothing().when(schedulingService).onCreated(any());
        lenient().doNothing().when(schedulingService).onUpdated(any(), any());
        lenient().doNothing().when(schedulingService).onCancelled(any());
    }

    @Nested
    class AdminPromotionRestControllerSecurityTest {

        @Test
        void testGetAllPromotions() throws Exception {
            testRights(get("/api/admin/promotions"), AuthorizationTest.ADMIN);
        }

        @Test
        void testGetPromotion() throws Exception {
            testRights(get("/api/admin/promotions/" + promotionId), AuthorizationTest.ADMIN);
        }

        @Test
        void testCreatePromotion() throws Exception {
            testRights(post("/api/admin/promotions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "name": "Summer Sale",
                            "startsAt": "2025-07-01T00:00:00Z",
                            "endsAt": "2025-07-31T23:59:59Z",
                            "priceCents": 999,
                            "bonusEveryN": 5
                        }
                    """), AuthorizationTest.ADMIN);
        }

        @Test
        void testUpdatePromotion() throws Exception {
            testRights(put("/api/admin/promotions/" + promotionId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "name": "Updated Sale",
                            "startsAt": "2025-08-01T00:00:00Z",
                            "endsAt": "2025-08-31T23:59:59Z",
                            "priceCents": 1299,
                            "bonusEveryN": 3
                        }
                    """), AuthorizationTest.ADMIN);
        }

        @Test
        void testCancelPromotion() throws Exception {
            testRights(delete("/api/admin/promotions/" + promotionId), AuthorizationTest.ADMIN);
        }
    }

    private void testRights(MockHttpServletRequestBuilder requestBuilder, AuthorizationTest authorization) throws Exception {
        var expected = status().isForbidden();

        var expectedForUnauthenticated = authorization.equals(AuthorizationTest.UNAUTHENTICATED)
                ? status().is2xxSuccessful()
                : status().isUnauthorized();
        mockMvc.perform(requestBuilder).andExpect(expectedForUnauthenticated);

        if (authorization.equals(AuthorizationTest.MEMBER) || authorization.equals(AuthorizationTest.UNAUTHENTICATED)) {
            expected = status().is2xxSuccessful();
        }
        mockMvc.perform(requestBuilder.with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER")))).andExpect(expected);

        if (authorization.equals(AuthorizationTest.EMAIL_VERIFIED)) {
            expected = status().is2xxSuccessful();
        }
        mockMvc.perform(requestBuilder.with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);

        if (authorization.equals(AuthorizationTest.MODERATOR)) {
            expected = status().is2xxSuccessful();
        }
        mockMvc.perform(requestBuilder.with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_MODERATOR"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);

        if (authorization.equals(AuthorizationTest.ADMIN)) {
            expected = status().is2xxSuccessful();
        }
        mockMvc.perform(requestBuilder.with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("EMAIL_VERIFIED")))).andExpect(expected);
    }

    private Promotion createPromotion(UUID id) {
        return new Promotion(
                id,
                "Summer Sale",
                Instant.parse("2025-07-01T00:00:00Z"),
                Instant.parse("2025-07-31T23:59:59Z"),
                999,
                5,
                PromotionStatus.ACTIVE,
                Instant.now()
        );
    }
}