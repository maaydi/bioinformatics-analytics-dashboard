package com.bioinformatics.dashboard.savedfilter.controller;

import com.bioinformatics.dashboard.auth.entity.AppUser;
import com.bioinformatics.dashboard.batch.AsyncUniprotImportJobExecutor;
import com.bioinformatics.dashboard.exception.AccessDeniedException;
import com.bioinformatics.dashboard.exception.DuplicateFilterNameException;
import com.bioinformatics.dashboard.exception.ResourceNotFoundException;
import com.bioinformatics.dashboard.model.gene.GeneSearchRequest;
import com.bioinformatics.dashboard.model.gene.PagedResponse;
import com.bioinformatics.dashboard.savedfilter.dto.SavedFilterCreateRequest;
import com.bioinformatics.dashboard.savedfilter.dto.SavedFilterDto;
import com.bioinformatics.dashboard.savedfilter.service.SavedFilterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link SavedFilterController}.
 * <p>
 * Tests all three endpoints with success and error scenarios, covering:
 * - GET /api/saved-filters: list saved filters for current user
 * - POST /api/saved-filters: create a new saved filter
 * - DELETE /api/saved-filters/{id}: delete a saved filter
 * <p>
 * Authorization checks and edge cases are explicitly tested.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "app.rate-limiter.enabled=false")
@AutoConfigureMockMvc
class SavedFilterControllerTest {
    @Autowired
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    @MockitoBean
    private SavedFilterService service;

    @MockitoBean
    private AsyncUniprotImportJobExecutor asyncUniprotImportJobExecutor;

    @TestConfiguration
    @Profile("test")
    static class CacheTestConfig {
        @Bean
        @Primary
        public CacheManager cacheManager() {
            return new NoOpCacheManager();
        }
    }


    private AppUser testUser;
    private SavedFilterCreateRequest validRequest;
    private SavedFilterDto testFilterDto;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        testUser = AppUser.builder()
                .id(1L)
                .username("test_user")
                .password("encoded_password")
                .role("ROLE_USER")
                .createdAt(Instant.now())
                .build();
        // Setup valid request
        var geneSearchRequest = new GeneSearchRequest(
                "kinase", // globalSearch
                null, // accession
                null, // entryName
                null, // geneNamePrimary
                null, // proteinFullName
                true, // reviewed
                null, // organism
                null, // taxid
                null, // lineage
                null, // lengthMin
                null, // lengthMax
                null, // molecularWeightMin
                null, // molecularWeightMax
                null, // evidenceLevels
                null, // keywords
                null, // goTermId
                null, // goAspect
                null, // featureType
                null, // crossRefSource
                0, // page
                10, // size
                null, // sort
                null  // direction
        );
        validRequest = new SavedFilterCreateRequest("My Filter", geneSearchRequest);
        // Setup test filter DTO
        testFilterDto = new SavedFilterDto(
                1L,
                "My Filter",
                geneSearchRequest,
                Instant.now()
        );
    }

    // ====== GET /api/saved-filters ======
    @Test
    @DisplayName("GET /api/saved-filters - Should list filters for current user with ROLE_USER")
    @WithMockUser(roles = "USER")
    void listSavedFilters_withAuthenticatedUser_returnsFilters() throws Exception {
        var filters = List.of(testFilterDto);
        when(service.listForCurrentUser(any(AppUser.class), any(Integer.class), any(Integer.class))).thenReturn(new PagedResponse<>(filters, 0, 1, 1, 1));
        mockMvc.perform(get("/api/saved-filters")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].name", is("My Filter")))
                .andExpect(jsonPath("$.content[0].createdAt", notNullValue()));
        verify(service).listForCurrentUser(any(AppUser.class), any(Integer.class), any(Integer.class));
    }

    @Test
    @DisplayName("GET /api/saved-filters - Should list empty filters when user has none")
    @WithMockUser(roles = "USER")
    void listSavedFilters_withNoFilters_returnsEmptyList() throws Exception {
        when(service.listForCurrentUser(any(AppUser.class), any(Integer.class), any(Integer.class))).thenReturn(new PagedResponse<>(List.of(), 0, 1, 1, 1));
        mockMvc.perform(get("/api/saved-filters")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(0)));
        verify(service).listForCurrentUser(any(AppUser.class), any(Integer.class), any(Integer.class));
    }

    @Test
    @DisplayName("GET /api/saved-filters - Should list filters for ADMIN user")
    @WithMockUser(roles = "ADMIN")
    void listSavedFilters_withAdminRole_returnsFilters() throws Exception {
        var adminUser = AppUser.builder()
                .id(2L)
                .username("admin_user")
                .password("encoded_password")
                .role("ROLE_ADMIN")
                .createdAt(Instant.now())
                .build();
        var filters = List.of(testFilterDto);
        when(service.listForCurrentUser(any(AppUser.class), any(Integer.class), any(Integer.class))).thenReturn(new PagedResponse<>(filters, 0, 1, 1, 1));
        mockMvc.perform(get("/api/saved-filters")
                        .with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
        verify(service).listForCurrentUser(any(AppUser.class), any(Integer.class), any(Integer.class));
    }

    @Test
    @DisplayName("GET /api/saved-filters - Should return 403 Forbidden without authentication")
    void listSavedFilters_withoutAuthentication_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/saved-filters"))
                .andExpect(status().isForbidden());
        verify(service, never()).listForCurrentUser(any(AppUser.class), any(Integer.class), any(Integer.class));
    }

    @Test
    @DisplayName("GET /api/saved-filters - Should return multiple filters ordered correctly")
    @WithMockUser(roles = "USER")
    void listSavedFilters_withMultipleFilters_returnsAll() throws Exception {
        var filter2 = new SavedFilterDto(
                2L,
                "Filter 2",
                validRequest.filterJson(),
                Instant.now().plusSeconds(60)
        );
        var filter3 = new SavedFilterDto(
                3L,
                "Filter 3",
                validRequest.filterJson(),
                Instant.now().plusSeconds(120)
        );
        var filters = List.of(testFilterDto, filter2, filter3);
        when(service.listForCurrentUser(any(AppUser.class), any(Integer.class), any(Integer.class))).thenReturn(new PagedResponse<>(filters, 0, 1, 3, 3));
        mockMvc.perform(get("/api/saved-filters")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].name", is("My Filter")))
                .andExpect(jsonPath("$.content[1].name", is("Filter 2")))
                .andExpect(jsonPath("$.content[2].name", is("Filter 3")));
        verify(service).listForCurrentUser(any(AppUser.class), any(Integer.class), any(Integer.class));
    }

    // ====== POST /api/saved-filters ======
    @Test
    @DisplayName("POST /api/saved-filters - Should create filter with valid request")
    @WithMockUser(roles = "USER")
    void createSavedFilter_withValidRequest_returnsCreatedFilter() throws Exception {
        when(service.create(any(SavedFilterCreateRequest.class), any(AppUser.class)))
                .thenReturn(testFilterDto);
        var requestJson = objectMapper.writeValueAsString(validRequest);
        mockMvc.perform(post("/api/saved-filters")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("My Filter")))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
        verify(service).create(any(SavedFilterCreateRequest.class), any(AppUser.class));
    }

    @Test
    @DisplayName("POST /api/saved-filters - Should reject request with empty name")
    @WithMockUser(roles = "USER")
    void createSavedFilter_withEmptyName_returnsBadRequest() throws Exception {
        var invalidRequest = new SavedFilterCreateRequest("", validRequest.filterJson());
        var requestJson = objectMapper.writeValueAsString(invalidRequest);
        mockMvc.perform(post("/api/saved-filters")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
        verify(service, never()).create(any(SavedFilterCreateRequest.class), any(AppUser.class));
    }

    @Test
    @DisplayName("POST /api/saved-filters - Should reject request with null name")
    @WithMockUser(roles = "USER")
    void createSavedFilter_withNullName_returnsBadRequest() throws Exception {
        var invalidRequest = new SavedFilterCreateRequest(null, validRequest.filterJson());
        var requestJson = objectMapper.writeValueAsString(invalidRequest);
        mockMvc.perform(post("/api/saved-filters")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
        verify(service, never()).create(any(SavedFilterCreateRequest.class), any(AppUser.class));
    }

    @Test
    @DisplayName("POST /api/saved-filters - Should reject name exceeding 100 characters")
    @WithMockUser(roles = "USER")
    void createSavedFilter_withNameTooLong_returnsBadRequest() throws Exception {
        var longName = "a".repeat(101);
        var invalidRequest = new SavedFilterCreateRequest(longName, validRequest.filterJson());
        var requestJson = objectMapper.writeValueAsString(invalidRequest);
        mockMvc.perform(post("/api/saved-filters")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
        verify(service, never()).create(any(SavedFilterCreateRequest.class), any(AppUser.class));
    }

    @Test
    @DisplayName("POST /api/saved-filters - Should reject request with null filterJson")
    @WithMockUser(roles = "USER")
    void createSavedFilter_withNullFilterJson_returnsBadRequest() throws Exception {
        var invalidRequest = new SavedFilterCreateRequest("Valid Name", null);
        var requestJson = objectMapper.writeValueAsString(invalidRequest);
        mockMvc.perform(post("/api/saved-filters")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
        verify(service, never()).create(any(SavedFilterCreateRequest.class), any(AppUser.class));
    }

    @Test
    @DisplayName("POST /api/saved-filters - Should handle duplicate filter name exception")
    @WithMockUser(roles = "USER")
    void createSavedFilter_withDuplicateName_returnsConflict() throws Exception {
        when(service.create(any(SavedFilterCreateRequest.class), any(AppUser.class)))
                .thenThrow(new DuplicateFilterNameException("Duplicated filter name My Filter", new Exception()));
        var requestJson = objectMapper.writeValueAsString(validRequest);
        mockMvc.perform(post("/api/saved-filters")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict());
        verify(service).create(any(SavedFilterCreateRequest.class), any(AppUser.class));
    }

    @Test
    @DisplayName("POST /api/saved-filters - Should return 403 Forbidden without authentication")
    void createSavedFilter_withoutAuthentication_returnsForbidden() throws Exception {
        var requestJson = objectMapper.writeValueAsString(validRequest);
        mockMvc.perform(post("/api/saved-filters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden());
        verify(service, never()).create(any(SavedFilterCreateRequest.class), any(AppUser.class));
    }

    @Test
    @DisplayName("POST /api/saved-filters - Should create filter with ROLE_ADMIN")
    @WithMockUser(roles = "ADMIN")
    void createSavedFilter_withAdminRole_returnsCreatedFilter() throws Exception {
        var adminUser = AppUser.builder()
                .id(2L)
                .username("admin_user")
                .password("encoded_password")
                .role("ROLE_ADMIN")
                .createdAt(Instant.now())
                .build();
        when(service.create(any(SavedFilterCreateRequest.class), any(AppUser.class)))
                .thenReturn(testFilterDto);
        var requestJson = objectMapper.writeValueAsString(validRequest);
        mockMvc.perform(post("/api/saved-filters")
                        .with(user(adminUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)));
        verify(service).create(any(SavedFilterCreateRequest.class), any(AppUser.class));
    }

    @Test
    @DisplayName("POST /api/saved-filters - Should handle malformed JSON gracefully")
    @WithMockUser(roles = "USER")
    void createSavedFilter_withMalformedJson_returnsServerError() throws Exception {
        mockMvc.perform(post("/api/saved-filters")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isInternalServerError());
        verify(service, never()).create(any(SavedFilterCreateRequest.class), any(AppUser.class));
    }

    // ====== DELETE /api/saved-filters/{id} ======
    @Test
    @DisplayName("DELETE /api/saved-filters/{id} - Should delete filter owned by current user")
    @WithMockUser(roles = "USER")
    void deleteSavedFilter_withValidId_returnsNoContent() throws Exception {
        doNothing().when(service).delete(eq(1L), any(AppUser.class));
        mockMvc.perform(delete("/api/saved-filters/1")
                        .with(user(testUser)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(service).delete(eq(1L), any(AppUser.class));
    }

    @Test
    @DisplayName("DELETE /api/saved-filters/{id} - Should return 404 when filter not found")
    @WithMockUser(roles = "USER")
    void deleteSavedFilter_withNonExistentId_returnsNotFound() throws Exception {
        doThrow(ResourceNotFoundException.forSavedFilter(999L))
                .when(service).delete(eq(999L), any(AppUser.class));
        mockMvc.perform(delete("/api/saved-filters/999")
                        .with(user(testUser)))
                .andExpect(status().isNotFound());
        verify(service).delete(eq(999L), any(AppUser.class));
    }

    @Test
    @DisplayName("DELETE /api/saved-filters/{id} - Should return 403 when user lacks permission")
    @WithMockUser(roles = "USER")
    void deleteSavedFilter_withoutPermission_returnsForbidden() throws Exception {
        doThrow(new AccessDeniedException("You don't have permission to delete this filter"))
                .when(service).delete(eq(1L), any(AppUser.class));
        mockMvc.perform(delete("/api/saved-filters/1")
                        .with(user(testUser)))
                .andExpect(status().isForbidden());
        verify(service).delete(eq(1L), any(AppUser.class));
    }

    @Test
    @DisplayName("DELETE /api/saved-filters/{id} - Should return 403 Forbidden without authentication")
    void deleteSavedFilter_withoutAuthentication_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/saved-filters/1"))
                .andExpect(status().isForbidden());
        verify(service, never()).delete(any(Long.class), any(AppUser.class));
    }

    @Test
    @DisplayName("DELETE /api/saved-filters/{id} - Admin should delete any filter")
    @WithMockUser(roles = "ADMIN")
    void deleteSavedFilter_withAdminRole_deletesAnyFilter() throws Exception {
        var adminUser = AppUser.builder()
                .id(2L)
                .username("admin_user")
                .password("encoded_password")
                .role("ROLE_ADMIN")
                .createdAt(Instant.now())
                .build();
        doNothing().when(service).delete(eq(1L), any(AppUser.class));
        mockMvc.perform(delete("/api/saved-filters/1")
                        .with(user(adminUser)))
                .andExpect(status().isNoContent());
        verify(service).delete(eq(1L), any(AppUser.class));
    }

    @Test
    @DisplayName("DELETE /api/saved-filters/{id} - Should handle invalid ID format gracefully")
    @WithMockUser(roles = "USER")
    void deleteSavedFilter_withInvalidIdFormat_returnsServerError() throws Exception {
        mockMvc.perform(delete("/api/saved-filters/invalid")
                        .with(user(testUser)))
                .andExpect(status().isInternalServerError());
        verify(service, never()).delete(any(Long.class), any(AppUser.class));
    }

    @Test
    @DisplayName("DELETE /api/saved-filters/{id} - Should handle multiple sequential deletes")
    @WithMockUser(roles = "USER")
    void deleteSavedFilter_multipleDeletionsInSequence_succeeds() throws Exception {
        doNothing().when(service).delete(any(Long.class), any(AppUser.class));
        mockMvc.perform(delete("/api/saved-filters/1")
                        .with(user(testUser)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/saved-filters/2")
                        .with(user(testUser)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/saved-filters/3")
                        .with(user(testUser)))
                .andExpect(status().isNoContent());
        verify(service, times(3)).delete(any(Long.class), any(AppUser.class));
    }

    // ====== Authorization Tests ======
    @Test
    @DisplayName("All endpoints should require USER or ADMIN role")
    void allEndpoints_requiresAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/saved-filters"))
                .andExpect(status().isForbidden());
        var requestJson = objectMapper.writeValueAsString(validRequest);
        mockMvc.perform(post("/api/saved-filters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/saved-filters/1"))
                .andExpect(status().isForbidden());
        verify(service, never()).listForCurrentUser(any(AppUser.class), any(Integer.class), any(Integer.class));
        verify(service, never()).create(any(SavedFilterCreateRequest.class), any(AppUser.class));
        verify(service, never()).delete(any(Long.class), any(AppUser.class));
    }

    // ====== Response Content Type Tests ======
    @Test
    @DisplayName("GET /api/saved-filters - Should return JSON content type")
    @WithMockUser(roles = "USER")
    void listSavedFilters_shouldReturnJsonContentType() throws Exception {
        when(service.listForCurrentUser(any(AppUser.class), any(Integer.class), any(Integer.class))).thenReturn(new PagedResponse<>(List.of(testFilterDto), 0, 1, 1, 1));
        mockMvc.perform(get("/api/saved-filters")
                        .with(user(testUser))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("POST /api/saved-filters - Should return JSON content type")
    @WithMockUser(roles = "USER")
    void createSavedFilter_shouldReturnJsonContentType() throws Exception {
        when(service.create(any(SavedFilterCreateRequest.class), any(AppUser.class)))
                .thenReturn(testFilterDto);
        var requestJson = objectMapper.writeValueAsString(validRequest);
        mockMvc.perform(post("/api/saved-filters")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}
