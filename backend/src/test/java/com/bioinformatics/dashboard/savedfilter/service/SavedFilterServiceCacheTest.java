package com.bioinformatics.dashboard.savedfilter.service;

import com.bioinformatics.dashboard.auth.entity.AppUser;
import com.bioinformatics.dashboard.savedfilter.dto.SavedFilterCreateRequest;
import com.bioinformatics.dashboard.savedfilter.dto.SavedFilterDto;
import com.bioinformatics.dashboard.savedfilter.entity.SavedFilter;
import com.bioinformatics.dashboard.savedfilter.mapper.SavedFilterMapper;
import com.bioinformatics.dashboard.savedfilter.repository.SavedFilterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = SavedFilterServiceCacheTest.CacheTestConfiguration.class,
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
        }
)
@DisplayName("SavedFilterService Cache Integration Tests")
class SavedFilterServiceCacheTest {

    @MockitoBean
    private SavedFilterRepository repository;
    @MockitoBean
    private SavedFilterMapper mapper;
    @Autowired
    private SavedFilterService service;
    @Autowired
    private CacheManager cacheManager;
    private AppUser testUser;

    @BeforeEach
    void setUp() {
        Objects.requireNonNull(cacheManager.getCache("savedFilters")).clear();

        testUser = new AppUser();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole("ROLE_USER");
    }

    @Test
    @DisplayName("listForCurrentUser should cache the result for the given user")
    void listForCurrentUser_cachesResult() {
        var entity = new SavedFilter();
        var dto = new SavedFilterDto(1L, "My Filter", null, Instant.now());

        when(repository.findByOwner(eq(testUser), any()))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(mapper.toDto(any(SavedFilter.class))).thenReturn(dto);

        service.listForCurrentUser(testUser, 0, 20);
        verify(repository, times(1)).findByOwner(eq(testUser), any());

        var cachedValue = Objects.requireNonNull(cacheManager.getCache("savedFilters")).get(testUser.getId());
        assertNotNull(cachedValue, "Cache should contain the paged response");

        service.listForCurrentUser(testUser, 0, 20);
        verify(repository, times(1)).findByOwner(eq(testUser), any()); // Still 1
    }

    @Test
    @DisplayName("create should evict the cache for the given user")
    void create_evictsCache() {
        var entity = new SavedFilter();
        var dto = new SavedFilterDto(1L, "My Filter", null, Instant.now());

        when(repository.findByOwner(eq(testUser), any()))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(repository.save(any())).thenReturn(entity);
        when(mapper.toEntity(any(), eq(testUser))).thenReturn(entity);
        when(mapper.toDto(any(SavedFilter.class))).thenReturn(dto);

        service.listForCurrentUser(testUser, 0, 20);
        assertNotNull(Objects.requireNonNull(cacheManager.getCache("savedFilters")).get(testUser.getId()));

        var request = new SavedFilterCreateRequest("New Filter", null);
        service.create(request, testUser);

        assertNull(Objects.requireNonNull(cacheManager.getCache("savedFilters"))
                .get(testUser.getId()), "Cache should be empty after creation");
    }

    @Test
    @DisplayName("deleteAndEvict should evict the cache for the given user")
    void deleteAndEvict_evictsCache() {
        var entity = new SavedFilter();

        when(repository.findByOwner(eq(testUser), any()))
                .thenReturn(new PageImpl<>(List.of(entity)));

        service.listForCurrentUser(testUser, 0, 20);
        assertNotNull(Objects.requireNonNull(cacheManager.getCache("savedFilters")).get(testUser.getId()));

        service.deleteAndEvict(1L, testUser);

        assertNull(Objects.requireNonNull(cacheManager.getCache("savedFilters"))
                .get(testUser.getId()), "Cache should be evicted after deletion");
        verify(repository, times(1)).deleteById(1L);
    }

    @Configuration
    @EnableCaching
    @Import(SavedFilterService.class)
    static class CacheTestConfiguration {
        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("savedFilters");
        }
    }
}
