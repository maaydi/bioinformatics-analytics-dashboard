package com.bioinformatics.authservice.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserStatusTest {

    @Test
    void userStatusEnumValues() {
        assertThat(UserStatus.CREATED).isNotNull();
        assertThat(UserStatus.ACTIVE).isNotNull();
        assertThat(UserStatus.DISABLED).isNotNull();
        assertThat(UserStatus.DELETED).isNotNull();
    }

    @Test
    void userStatusCanBeValueOf() {
        assertThat(UserStatus.valueOf("ACTIVE")).isEqualTo(UserStatus.ACTIVE);
        assertThat(UserStatus.valueOf("DELETED")).isEqualTo(UserStatus.DELETED);
    }

    @Test
    void userStatusCanBeConvertedToString() {
        assertThat(UserStatus.ACTIVE.toString()).isEqualTo("ACTIVE");
        assertThat(UserStatus.CREATED.toString()).isEqualTo("CREATED");
    }

    @Test
    void userStatusHasAllStates() {
        var statuses = UserStatus.values();
        assertThat(statuses).hasSize(4);
    }
}

