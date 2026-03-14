package fr.uge.forkeat.presentation.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import fr.uge.forkeat.presentation.dto.superlike.SuperLikeConfigDTO;
import fr.uge.forkeat.presentation.dto.superlike.UpdateSuperLikeConfigDTO;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.PromotionService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminSuperLikeConfigRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminSuperLikeConfigRestControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean
    private PromotionService promotionService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private AuthenticationPort authPort;

    @Autowired
    AdminSuperLikeConfigRestControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    private SuperLikeConfig defaultConfig() {
        return new SuperLikeConfig(UUID.randomUUID(), 100L, new BigDecimal("0.40"), Instant.now());
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
}
