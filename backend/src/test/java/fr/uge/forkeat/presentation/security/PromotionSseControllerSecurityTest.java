package fr.uge.forkeat.presentation.security;

import fr.uge.forkeat.infrastructure.AbstractIntegrationTest;
import fr.uge.forkeat.infrastructure.sse.SsePromotionNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("test")
@Transactional
public class PromotionSseControllerSecurityTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SsePromotionNotifier sseNotifier;

    @BeforeEach
    void setup() {
        when(sseNotifier.subscribe()).thenReturn(new SseEmitter());
    }

    @Nested
    class PromotionSseControllerSecurityTests {

        @Test
        void testStream() throws Exception {
            testRights(get("/api/promotions/stream"), AuthorizationTest.UNAUTHENTICATED);
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
}