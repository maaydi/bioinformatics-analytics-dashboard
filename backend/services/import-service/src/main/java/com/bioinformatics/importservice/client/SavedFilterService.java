package com.bioinformatics.importservice.client;

import com.bioinformatics.common.models.filter.SavedFilterDto;
import com.bioinformatics.shared.models.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavedFilterService {
    private final SavedFilterClient client;

    public Optional<SavedFilterDto> getSavedFilterById(Long id, UserPrincipal user) {
        var response = client.getSavedFilterById(id, user.id(), user.roles());
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return Optional.of(response.getBody());
        }
        return Optional.empty();

    }
}
