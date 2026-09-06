package com.bioinformatics.shared.models.security;

import java.util.List;

import static com.bioinformatics.shared.models.security.Constants.ADMIN_ROLE;

public record UserPrincipal(String id, List<String> roles, String dataProvider) {

    public boolean isAdmin() {
        return roles.stream().anyMatch(e -> e.contains(ADMIN_ROLE));
    }
}
