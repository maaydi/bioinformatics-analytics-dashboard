package com.bioinformatics.dashboard.savedfilter.service;

import com.bioinformatics.dashboard.auth.entity.AppUser;
import com.bioinformatics.dashboard.exception.AccessDeniedException;
import com.bioinformatics.dashboard.exception.DuplicateFilterNameException;
import com.bioinformatics.dashboard.exception.ResourceNotFoundException;
import com.bioinformatics.dashboard.savedfilter.dto.SavedFilterCreateRequest;
import com.bioinformatics.dashboard.savedfilter.dto.SavedFilterDto;
import com.bioinformatics.dashboard.savedfilter.mapper.SavedFilterMapper;
import com.bioinformatics.dashboard.savedfilter.repository.SavedFilterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavedFilterService {
    private final SavedFilterRepository repository;
    private final SavedFilterMapper mapper;

    public List<SavedFilterDto> listForCurrentUser(AppUser currentUser) {
        return repository.findByOwnerOrderByCreatedAtDesc(currentUser)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    public SavedFilterDto create(SavedFilterCreateRequest request, AppUser owner) {
        log.info("Save filter <{}> created by <{}>", request.name(), owner.getUsername());
        try {
            var entity = mapper.toEntity(request, owner);
            var res = repository.save(entity);
            log.info("Filter <{}> created by <{}> successfully saved", request.name(), owner.getUsername());
            return mapper.toDto(res);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateFilterNameException("Duplicated filter name %s".formatted(request.name()), ex);
        } catch (Exception e) {
            log.error("An error occurs while trying to saved filter: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void delete(Long id, AppUser currentUser) {
        var filter = repository.findById(id);
        if (filter.isEmpty()) {
            throw ResourceNotFoundException.forSavedFilter(id);
        } else if (!filter.get().getOwner().getUsername().equals(currentUser.getUsername())) {
            throw new AccessDeniedException("You don't have permission to delete this filter");
        } else {
            repository.delete(filter.get());
        }
    }


}
