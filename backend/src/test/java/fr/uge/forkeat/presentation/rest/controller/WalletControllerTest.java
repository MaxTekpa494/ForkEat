package fr.uge.forkeat.presentation.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.uge.forkeat.infrastructure.config.JwtFilter;
import fr.uge.forkeat.presentation.dto.user.TopUpRequestDTO;
import fr.uge.forkeat.service.CustomUserDetailsService;
import fr.uge.forkeat.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalletRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private WalletService walletService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void shouldReturnPaymentUrl() throws Exception {
        var request = new TopUpRequestDTO(1000L); // Ton DTO
        var expectedUrl = "https://checkout.stripe.com/pay/123";

        var hardcodedUserId = UUID.fromString("0a6a42d0-696b-4fa6-aa8e-40ba65a660ca");

        when(walletService.prepareTopUp(
                eq(hardcodedUserId),
                eq("test@user.com"),
                eq(1000L)
        )).thenReturn(expectedUrl);

        mockMvc.perform(post("/wallet/recharge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(expectedUrl));
    }
}