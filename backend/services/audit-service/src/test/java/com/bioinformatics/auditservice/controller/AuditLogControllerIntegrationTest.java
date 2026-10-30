package com.bioinformatics.auditservice.controller;

import com.bioinformatics.auditservice.service.AuditService;
import com.bioinformatics.auditservice.support.AuditIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static com.bioinformatics.auditservice.support.AuditTestFixtures.event;
import static com.bioinformatics.shared.models.security.Constants.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class AuditLogControllerIntegrationTest extends AuditIntegrationTest {

    private static final String AUDIT_PATH = "/api/v1/admin/audit";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private AuditService service;

    @Test
    void adminSearchReturnsPersistedFieldsAndPaginationEnvelope() throws Exception {
        var first = service.save(event("attempts", 2));
        service.save(event("attempts", 3));

        mvc.perform(post(AUDIT_PATH).header(USER_ID_HEADER, "admin").header(USER_ROLE_HEADER, ADMIN_ROLE)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"actorIdentities":["alice"],"metadata":{"attempts":[2,3]},
                                 "page":1,"size":1,"sort":"id","direction":"desc"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(first.id()))
                .andExpect(jsonPath("$.content[0].eventId").value(first.eventId().toString()))
                .andExpect(jsonPath("$.content[0].actorIdentity").value("alice"))
                .andExpect(jsonPath("$.content[0].actorType").value("USER"))
                .andExpect(jsonPath("$.content[0].sourceService").value("auth-service"))
                .andExpect(jsonPath("$.content[0].correlationId").value("request-123"))
                .andExpect(jsonPath("$.content[0].action").value("LOGIN"))
                .andExpect(jsonPath("$.content[0].targetType").value("AUTH"))
                .andExpect(jsonPath("$.content[0].targetId").value("alice"))
                .andExpect(jsonPath("$.content[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.content[0].ipAddress").value("192.0.2.10"))
                .andExpect(jsonPath("$.content[0].metadata.attempts").value(2))
                .andExpect(jsonPath("$.content[0].createdAt").value(first.createdAt().toString()))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void noMatchesReturnsAnEmptyPageWithTheRequestedSize() throws Exception {
        mvc.perform(post(AUDIT_PATH).header(USER_ID_HEADER, "admin").header(USER_ROLE_HEADER, ADMIN_ROLE)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"actorIdentities":["nobody"],"page":0,"size":10}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"page\":-1,\"size\":20}",
            "{\"page\":0,\"size\":0}",
            "{\"page\":0,\"size\":201}",
            "{\"page\":0,\"size\":20,\"direction\":\"sideways\"}",
            """
                    {"page":0,"size":20,"startDate":"2026-01-16T00:00:00Z","endDate":"2026-01-15T00:00:00Z"}
                    """
    })
    void invalidSearchParametersReturnTheSharedBadRequestEnvelope(String body) throws Exception {
        mvc.perform(post(AUDIT_PATH).header(USER_ID_HEADER, "admin").header(USER_ROLE_HEADER, ADMIN_ROLE)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void anonymousSearchCannotReadPersistedAuditData() throws Exception {
        service.save(event());

        mvc.perform(post(AUDIT_PATH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"page\":0,\"size\":20}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonAdminSearchCannotReadPersistedAuditData() throws Exception {
        service.save(event());

        mvc.perform(post(AUDIT_PATH).header(USER_ID_HEADER, "alice").header(USER_ROLE_HEADER, USER_ROLE)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"page\":0,\"size\":20}"))
                .andExpect(status().isForbidden());
    }
}
