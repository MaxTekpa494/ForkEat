package fr.uge.forkeat.presentation.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.uge.forkeat.infrastructure.scheduler.PromotionSchedulingService;
import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import fr.uge.forkeat.presentation.dto.superlike.*;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.PromotionService;
import fr.uge.forkeat.service.exception.PromotionModificationForbiddenException;
import fr.uge.forkeat.service.exception.PromotionNotFoundException;
import fr.uge.forkeat.service.exception.PromotionNotProfitableException;
import fr.uge.forkeat.service.exception.PromotionOverlapException;
import fr.uge.forkeat.service.model.superlike.Promotion;
import fr.uge.forkeat.service.model.superlike.PromotionStatus;
import fr.uge.forkeat.service.model.superlike.SuperLikeConfig;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminSuperLikeRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminSuperLikeRestControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean
    private PromotionService promotionService;

    @MockitoBean
    private PromotionSchedulingService schedulingService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private AuthenticationPort authPort;

    @Autowired
    AdminSuperLikeRestControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    private SuperLikeConfig defaultConfig() {
        return new SuperLikeConfig(UUID.randomUUID(), 100L, new BigDecimal("0.40"), Instant.now());
    }

    private Promotion scheduledPromo(UUID id) {
        var now = Instant.now();
        return new Promotion(id, "Promo", now.plusSeconds(60), now.plusSeconds(3660), 100L, null, PromotionStatus.SCHEDULED, now);
    }

    // ─── GET /api/admin/super-like/config ────────────────────────────────

    @Nested
    class GetConfig {

        @Test
        void shouldReturn200WithConfig() throws Exception {
            when(promotionService.getConfig()).thenReturn(defaultConfig());

            mockMvc.perform(get("/api/admin/super-like/config"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resource.priceCents").value(100))
                    .andExpect(jsonPath("$.resource.earningsRatio").value(0.40));
        }
    }

    // ─── PUT /api/admin/super-like/config ────────────────────────────────

    @Nested
    class UpdateConfig {

        @Test
        void shouldReturn200WithUpdatedConfig() throws Exception {
            var dto = new UpdateSuperLikeConfigDTO(200L, new BigDecimal("0.50"));
            var updated = new SuperLikeConfig(UUID.randomUUID(), 200L, new BigDecimal("0.50"), Instant.now());
            when(promotionService.updateConfig(200L, new BigDecimal("0.50"))).thenReturn(updated);

            mockMvc.perform(put("/api/admin/super-like/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resource.priceCents").value(200));
        }

        @Test
        void shouldReturn400_whenPriceCentsIsZero() throws Exception {
            var dto = new UpdateSuperLikeConfigDTO(0L, new BigDecimal("0.40"));
            // Stub lenient : ne doit pas être appelé si @Valid bloque la requête
            lenient().when(promotionService.updateConfig(anyLong(), any())).thenReturn(defaultConfig());

            mockMvc.perform(put("/api/admin/super-like/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn400_whenEarningsRatioIsNull() throws Exception {
            var body = "{\"priceCents\":100}";
            lenient().when(promotionService.updateConfig(anyLong(), any())).thenReturn(defaultConfig());

            mockMvc.perform(put("/api/admin/super-like/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─── GET /api/admin/promotions ────────────────────────────────────────

    @Nested
    class GetAllPromotions {

        @Test
        void shouldReturn200WithList() throws Exception {
            var id = UUID.randomUUID();
            when(promotionService.findAll()).thenReturn(List.of(scheduledPromo(id)));

            mockMvc.perform(get("/api/admin/promotions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resources[0].name").value("Promo"))
                    .andExpect(jsonPath("$.resources[0].status").value("SCHEDULED"));
        }

        @Test
        void shouldReturn200WithEmptyList() throws Exception {
            when(promotionService.findAll()).thenReturn(List.of());

            mockMvc.perform(get("/api/admin/promotions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resources").isEmpty())
                    .andExpect(jsonPath("$.total").value(0));
        }
    }

    // ─── GET /api/admin/promotions/{id} ──────────────────────────────────

    @Nested
    class GetPromotion {

        @Test
        void shouldReturn200_whenFound() throws Exception {
            var id = UUID.randomUUID();
            when(promotionService.findById(id)).thenReturn(scheduledPromo(id));

            mockMvc.perform(get("/api/admin/promotions/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resource.name").value("Promo"))
                    .andExpect(jsonPath("$.resource.status").value("SCHEDULED"));
        }

        @Test
        void shouldReturn404_whenNotFound() throws Exception {
            var id = UUID.randomUUID();
            when(promotionService.findById(id)).thenThrow(new PromotionNotFoundException(id));

            mockMvc.perform(get("/api/admin/promotions/{id}", id))
                    .andExpect(status().isNotFound());
        }
    }

    // ─── POST /api/admin/promotions ───────────────────────────────────────

    @Nested
    class CreatePromotion {

        @Test
        void shouldReturn201_whenCreated() throws Exception {
            var id = UUID.randomUUID();
            var now = Instant.now();
            var dto = new CreatePromotionDTO("Promo", now.plusSeconds(60), now.plusSeconds(3660), 100L, null);
            when(promotionService.create(any(), any(), any(), anyLong(), isNull())).thenReturn(scheduledPromo(id));

            mockMvc.perform(post("/api/admin/promotions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.resource.name").value("Promo"))
                    .andExpect(jsonPath("$.resource.status").value("SCHEDULED"));
        }

        @Test
        void shouldReturn400_whenEndsAtIsNull() throws Exception {
            var now = Instant.now();
            // endsAt absent → @NotNull échoue → 400
            var body = String.format(
                    "{\"name\":\"Promo\",\"startsAt\":\"%s\",\"priceCents\":100}",
                    now.plusSeconds(60)
            );
            lenient().when(promotionService.create(any(), any(), any(), anyLong(), any()))
                    .thenReturn(scheduledPromo(UUID.randomUUID()));

            mockMvc.perform(post("/api/admin/promotions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn400_whenNameIsBlank() throws Exception {
            var now = Instant.now();
            var dto = new CreatePromotionDTO("", now.plusSeconds(60), now.plusSeconds(3660), 100L, null);
            lenient().when(promotionService.create(any(), any(), any(), anyLong(), any()))
                    .thenReturn(scheduledPromo(UUID.randomUUID()));

            mockMvc.perform(post("/api/admin/promotions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn409_whenOverlap() throws Exception {
            var now = Instant.now();
            var dto = new CreatePromotionDTO("Promo", now.plusSeconds(60), now.plusSeconds(3660), 100L, null);
            when(promotionService.create(any(), any(), any(), anyLong(), isNull()))
                    .thenThrow(new PromotionOverlapException());

            mockMvc.perform(post("/api/admin/promotions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isConflict());
        }

        @Test
        void shouldReturn422_whenNotProfitable() throws Exception {
            var now = Instant.now();
            var dto = new CreatePromotionDTO("Promo", now.plusSeconds(60), now.plusSeconds(3660), 100L, 5);
            when(promotionService.create(any(), any(), any(), anyLong(), eq(5)))
                    .thenThrow(new PromotionNotProfitableException(5, 0.10));

            mockMvc.perform(post("/api/admin/promotions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    // ─── PUT /api/admin/promotions/{id} ──────────────────────────────────

    @Nested
    class UpdatePromotion {

        @Test
        void shouldReturn200_whenUpdated() throws Exception {
            var id = UUID.randomUUID();
            var now = Instant.now();
            var dto = new UpdatePromotionDTO("Updated", null, null, 200L, null);
            var updated = new Promotion(id, "Updated", now.plusSeconds(60), now.plusSeconds(3660), 200L, null, PromotionStatus.SCHEDULED, now);
            when(promotionService.update(eq(id), eq("Updated"), isNull(), isNull(), eq(200L), isNull()))
                    .thenReturn(updated);

            mockMvc.perform(put("/api/admin/promotions/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resource.name").value("Updated"))
                    .andExpect(jsonPath("$.resource.priceCents").value(200));
        }

        @Test
        void shouldReturn404_whenNotFound() throws Exception {
            var id = UUID.randomUUID();
            var dto = new UpdatePromotionDTO("X", null, null, null, null);
            when(promotionService.update(eq(id), eq("X"), isNull(), isNull(), isNull(), isNull()))
                    .thenThrow(new PromotionNotFoundException(id));

            mockMvc.perform(put("/api/admin/promotions/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturn422_whenModificationForbidden() throws Exception {
            var id = UUID.randomUUID();
            var dto = new UpdatePromotionDTO(null, null, null, null, null);
            when(promotionService.update(eq(id), isNull(), isNull(), isNull(), isNull(), isNull()))
                    .thenThrow(new PromotionModificationForbiddenException(PromotionStatus.ACTIVE));

            mockMvc.perform(put("/api/admin/promotions/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    // ─── DELETE /api/admin/promotions/{id} ───────────────────────────────

    @Nested
    class CancelPromotion {

        @Test
        void shouldReturn204_whenCancelled() throws Exception {
            var id = UUID.randomUUID();
            doNothing().when(promotionService).cancel(id);

            mockMvc.perform(delete("/api/admin/promotions/{id}", id))
                    .andExpect(status().isNoContent());

            verify(promotionService).cancel(id);
        }

        @Test
        void shouldReturn404_whenNotFound() throws Exception {
            var id = UUID.randomUUID();
            doThrow(new PromotionNotFoundException(id)).when(promotionService).cancel(id);

            mockMvc.perform(delete("/api/admin/promotions/{id}", id))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturn422_whenModificationForbidden() throws Exception {
            var id = UUID.randomUUID();
            doThrow(new PromotionModificationForbiddenException(PromotionStatus.ACTIVE))
                    .when(promotionService).cancel(id);

            mockMvc.perform(delete("/api/admin/promotions/{id}", id))
                    .andExpect(status().isUnprocessableEntity());
        }
    }
}
