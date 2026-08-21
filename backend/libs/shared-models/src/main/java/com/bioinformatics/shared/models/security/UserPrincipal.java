package com.bioinformatics.shared.models.security;

import static com.bioinformatics.shared.models.security.Constants.ADMIN_ROLE;

public record UserPrincipal(String id, String role, String dataProvider) {

    public boolean isAdmin() {
        return role.contains(ADMIN_ROLE);
    }
}
