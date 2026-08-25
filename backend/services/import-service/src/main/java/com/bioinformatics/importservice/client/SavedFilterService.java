package com.bioinformatics.importservice.client;

import com.bioinformatics.common.models.filter.SavedFilterDto;
import com.bioinformatics.shared.models.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavedFilterService {
    private final SavedFilterClient client;

    public Optional<SavedFilterDto> getSavedFilterById(Long id) {
        var user = getCurrentUser();
        if (user == null) {
            throw new BadCredentialsException("User is not logged in");
        }
        var response = client.getSavedFilterById(id, user.id(), user.roles().getFirst());
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return Optional.of(response.getBody());
        }
        return Optional.empty();

    }

    private UserPrincipal getCurrentUser() {
        UserPrincipal usr = null;
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal user) {
            usr = user;
        }
        return usr;
    }
}
