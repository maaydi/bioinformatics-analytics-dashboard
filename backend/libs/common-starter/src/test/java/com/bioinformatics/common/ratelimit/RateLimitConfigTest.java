package com.bioinformatics.common.ratelimit;

import com.bioinformatics.common.ratelimit.aspect.RateLimitAspect;
import com.bioinformatics.common.ratelimit.config.RateLimitConfig;
import com.bioinformatics.common.ratelimit.config.RateLimitProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitConfigTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withUserConfiguration(RateLimitConfig.class);

    @Test
    void enablesServletLimiterWithDefaultPolicyAndNoEndpointList() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(RateLimitAspect.class);
            var properties = context.getBean(RateLimitProperties.class);
            assertThat(properties.enabled()).isTrue();
            assertThat(properties.global()).isEqualTo(new RateLimitProperties.Settings("global", 100, 100, 60));
            assertThat(properties.endpoints()).isEmpty();
        });
    }

    @Test
    void bindsExistingAppNamespaceIndependentlyOfServiceProperties() {
        runner.withPropertyValues("app.rate-limiter.global.capacity=200", "app.rate-limiter.global.tokens=50",
                        "app.rate-limiter.global.seconds=30", "app.rate-limiter.endpoints[0].name=login",
                        "app.rate-limiter.endpoints[0].capacity=10", "app.rate-limiter.endpoints[0].tokens=5",
                        "app.rate-limiter.endpoints[0].seconds=60")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var properties = context.getBean(RateLimitProperties.class);
                    assertThat(properties.global()).isEqualTo(new RateLimitProperties.Settings("global", 200, 50, 30));
                    assertThat(properties.endpoints()).containsExactly(new RateLimitProperties.Settings("login", 10, 5, 60));
                });
    }

    @Test
    void disablingLimiterDoesNotBindOrValidateUnusedPolicies() {
        runner.withPropertyValues("app.rate-limiter.enabled=false", "app.rate-limiter.global.capacity=0")
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(RateLimitAspect.class)
                        .doesNotHaveBean(RateLimitProperties.class));
    }

    @Test
    void doesNotActivateOutsideServletApplications() {
        new ApplicationContextRunner().withUserConfiguration(RateLimitConfig.class)
                .run(context -> assertThat(context).doesNotHaveBean(RateLimitAspect.class)
                        .doesNotHaveBean(RateLimitProperties.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"capacity=0", "capacity=-1", "tokens=0", "tokens=-1", "seconds=0", "seconds=-1", "name="})
    void rejectsInvalidPolicyOnStartup(String setting) {
        runner.withPropertyValues("app.rate-limiter.global." + setting)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsDuplicateEndpointPoliciesOnStartup() {
        runner.withPropertyValues("app.rate-limiter.endpoints[0].name=search", "app.rate-limiter.endpoints[1].name=search")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsEndpointOverridingReservedGlobalPolicy() {
        runner.withPropertyValues("app.rate-limiter.endpoints[0].name=global")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsRenamedGlobalPolicy() {
        runner.withPropertyValues("app.rate-limiter.global.name=renamed")
                .run(context -> assertThat(context).hasFailed());
    }
}
