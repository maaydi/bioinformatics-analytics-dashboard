package com.bioinformatics.auditservice.config;

import com.bioinformatics.auditservice.controller.AuditLogController;
import com.bioinformatics.auditservice.dto.AuditSearchRequest;
import com.bioinformatics.auditservice.service.AuditService;
import com.bioinformatics.common.config.security.GatewayUserAuthenticationFilter;
import com.bioinformatics.common.models.PagedResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;
import java.util.Map;

import static com.bioinformatics.shared.models.security.Constants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(SecurityConfigTest.Config.class)
@WebAppConfiguration
class SecurityConfigTest {

    private static final String AUDIT_PATH = "/api/v1/admin/audit";
    private static final String SEARCH_REQUEST = """
            {"page":0,"size":20,"metadata":{}}
            """;

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private FilterChainProxy securityFilterChain;
    @Autowired
    private AuditLogController controller;
    @MockitoBean
    private AuditService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilterChain).build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"})
    void healthProbesRemainPublic(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "ROLE_ADMIN"})
    void gatewayAdminCanSearchWithoutCsrfAndDoesNotCreateSession(String role) throws Exception {
        when(service.listAuditLogs(any(AuditSearchRequest.class)))
                .thenReturn(new PagedResponse<>(List.of(), 0, 20, 0L, 0));

        mvc.perform(post(AUDIT_PATH).contentType(MediaType.APPLICATION_JSON).content(SEARCH_REQUEST)
                        .header(USER_ID_HEADER, "admin")
                        .header(USER_ROLE_HEADER, role))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getRequest().getSession(false)).isNull());

        mvc.perform(post(AUDIT_PATH).contentType(MediaType.APPLICATION_JSON).content(SEARCH_REQUEST))
                .andExpect(status().isForbidden());
        verify(service).listAuditLogs(any(AuditSearchRequest.class));
    }

    @Test
    void anonymousAuditSearchIsForbidden() throws Exception {
        mvc.perform(post(AUDIT_PATH).contentType(MediaType.APPLICATION_JSON).content(SEARCH_REQUEST))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void nonAdminAuditSearchIsForbidden() throws Exception {
        mvc.perform(post(AUDIT_PATH).contentType(MediaType.APPLICATION_JSON).content(SEARCH_REQUEST)
                        .header(USER_ID_HEADER, "user")
                        .header(USER_ROLE_HEADER, USER_ROLE))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void roleHeaderWithoutUserIdentityDoesNotAuthenticate() throws Exception {
        mvc.perform(post(AUDIT_PATH).contentType(MediaType.APPLICATION_JSON).content(SEARCH_REQUEST)
                        .header(USER_ROLE_HEADER, ADMIN_ROLE))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void nonHealthEndpointsRequireAuthentication() throws Exception {
        mvc.perform(get("/actuator/info")).andExpect(status().isForbidden());
        mvc.perform(get("/actuator/info").header(USER_ID_HEADER, "user").header(USER_ROLE_HEADER, USER_ROLE))
                .andExpect(status().isOk());
    }

    @Test
    void methodSecurityProtectsDirectControllerInvocations() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("user", null,
                        AuthorityUtils.createAuthorityList("ROLE_USER")));

        assertThatThrownBy(() -> controller.listAuditLogs(AuditSearchRequest.builder().page(0).size(20).build()))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(service);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import({SecurityConfig.class, GatewayUserAuthenticationFilter.class, AuditLogController.class, ProbeController.class})
    static class Config {
    }

    @RestController
    static class ProbeController {

        @GetMapping({"/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"})
        Map<String, String> health() {
            return Map.of("status", "UP");
        }

        @GetMapping("/actuator/info")
        Map<String, String> info() {
            return Map.of();
        }
    }
}
