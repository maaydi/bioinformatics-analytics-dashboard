package com.bioinformatics.common.ratelimit;

import com.bioinformatics.common.exception.GlobalExceptionHandler;
import com.bioinformatics.common.exception.RateLimitExceededException;
import com.bioinformatics.common.ratelimit.annotation.RateLimited;
import com.bioinformatics.common.ratelimit.aspect.RateLimitAspect;
import com.bioinformatics.common.ratelimit.config.RateLimitConfig;
import com.bioinformatics.common.ratelimit.config.RateLimitProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RateLimitAspectTest {

    private TestController target;
    private TestController proxy;

    @BeforeEach
    void setUp() {
        target = new TestController();
        proxy = proxy(new RateLimitProperties(true, policy("global", 2, 3600),
                List.of(policy("search", 2, 3600), policy("detail", 1, 3600))));
        bindRequest("192.0.2.1");
    }

    @AfterEach
    void cleanUpRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void permitsExactlyCapacityThenRejectsBeforeInvokingTarget() {
        assertThat(proxy.search()).isEqualTo("ok");
        assertThat(proxy.search()).isEqualTo("ok");
        assertThatThrownBy(proxy::search).isInstanceOf(RateLimitExceededException.class);
        assertThat(target.calls.get()).isEqualTo(2);
    }

    @Test
    void methodsWithSamePolicyShareBudgetButDifferentPoliciesAreIndependent() {
        proxy.search();
        proxy.anotherSearch();
        assertThatThrownBy(proxy::search).isInstanceOf(RateLimitExceededException.class);
        assertThat(proxy.detail()).isEqualTo("ok");
        assertThat(proxy.defaultPolicy()).isEqualTo("ok");
    }

    @Test
    void clientsHaveIndependentBudgetsAndForwardedHeadersDoNotOverrideRemoteAddress() {
        proxy.detail();
        var request = bindRequest("192.0.2.1");
        request.addHeader("X-Forwarded-For", "192.0.2.2");
        assertThatThrownBy(proxy::detail).isInstanceOf(RateLimitExceededException.class);
        bindRequest("192.0.2.2");
        assertThat(proxy.detail()).isEqualTo("ok");
    }

    @Test
    void omittedKeyUsesGlobalBudget() {
        proxy.defaultPolicy();
        proxy.defaultPolicy();
        assertThatThrownBy(proxy::defaultPolicy).isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void independentInstancesDoNotShareBuckets() {
        proxy.detail();
        var anotherInstance = proxy(new RateLimitProperties(true, policy("global", 1, 3600),
                List.of(policy("detail", 1, 3600))));
        assertThat(anotherInstance.detail()).isEqualTo("ok");
    }

    @Test
    void unannotatedMethodsAreNotLimited() {
        IntStream.range(0, 10).forEach(_ -> assertThat(proxy.unlimited()).isEqualTo("ok"));
    }

    @Test
    void missingPolicyFailsExplicitlyInsteadOfFallingBackToGlobal() {
        assertThatThrownBy(proxy::missingPolicy).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No rate limit policy configured for: missing");
        assertThat(target.calls.get()).isZero();
    }

    @Test
    void annotatedMethodWithoutServletRequestFailsExplicitly() {
        RequestContextHolder.resetRequestAttributes();
        assertThatThrownBy(proxy::search).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requires a servlet request");
        assertThat(target.calls.get()).isZero();
    }

    @Test
    void failedBusinessCallsStillConsumeTokensAndPreserveException() {
        assertThatThrownBy(proxy::failing).isInstanceOf(IllegalArgumentException.class).hasMessage("business failure");
        assertThatThrownBy(proxy::detail).isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void greedilyRefillsConsumedTokens() {
        var refilling = proxy(new RateLimitProperties(true, policy("global", 1, 1), List.of()));
        refilling.defaultPolicy();
        assertThatThrownBy(refilling::defaultPolicy).isInstanceOf(RateLimitExceededException.class);
        await().pollInSameThread().ignoreException(RateLimitExceededException.class).atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(refilling.defaultPolicy()).isEqualTo("ok"));
    }

    @Test
    void concurrentCallsCannotExceedCapacity() throws Exception {
        var tasks = IntStream.range(0, 20).<Callable<Boolean>>mapToObj(_ -> () -> {
            bindRequest("192.0.2.1");
            try {
                proxy.search();
                return true;
            } catch (RateLimitExceededException ex) {
                return false;
            } finally {
                RequestContextHolder.resetRequestAttributes();
            }
        }).toList();
        try (var executor = Executors.newFixedThreadPool(8)) {
            var results = executor.invokeAll(tasks);
            var accepted = 0;
            for (var result : results) {
                if (result.get()) {
                    accepted++;
                }
            }
            assertThat(accepted).isEqualTo(2);
            assertThat(target.calls.get()).isEqualTo(2);
        }
    }

    @Test
    void servletAutoConfigurationEnforcesPolicyAndReturnsStandard429Body() {
        new WebApplicationContextRunner().withUserConfiguration(RateLimitConfig.class)
                .withBean(TestController.class)
                .withPropertyValues("app.rate-limiter.global.capacity=1", "app.rate-limiter.global.tokens=1",
                        "app.rate-limiter.global.seconds=3600")
                .run(context -> {
                    var mvc = MockMvcBuilders.standaloneSetup(context.getBean(TestController.class))
                            .setControllerAdvice(new GlobalExceptionHandler()).build();
                    mvc.perform(get("/limited")).andExpect(status().isOk());
                    mvc.perform(get("/limited")).andExpect(status().isTooManyRequests())
                            .andExpect(jsonPath("$.status").value(429))
                            .andExpect(jsonPath("$.error").value("Too Many Requests"))
                            .andExpect(jsonPath("$.message").value("Rate limit exceeded. Try again later."));
                });
    }

    @Test
    void disabledConfigurationLeavesAnnotatedControllerUnthrottled() {
        new WebApplicationContextRunner().withUserConfiguration(RateLimitConfig.class)
                .withBean(TestController.class)
                .withPropertyValues("app.rate-limiter.enabled=false", "app.rate-limiter.global.capacity=1")
                .run(context -> {
                    var mvc = MockMvcBuilders.standaloneSetup(context.getBean(TestController.class)).build();
                    for (var i = 0; i < 3; i++) {
                        mvc.perform(get("/limited")).andExpect(status().isOk());
                    }
                });
    }

    private TestController proxy(RateLimitProperties properties) {
        var factory = new AspectJProxyFactory(target);
        factory.addAspect(new RateLimitAspect(properties));
        return factory.getProxy();
    }

    private static RateLimitProperties.Settings policy(String name, long capacity, long seconds) {
        return new RateLimitProperties.Settings(name, capacity, capacity, seconds);
    }

    private static MockHttpServletRequest bindRequest(String clientIp) {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr(clientIp);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        return request;
    }

    @RestController
    static class TestController {
        private final AtomicInteger calls = new AtomicInteger();

        @RateLimited(key = "search")
        public String search() {
            calls.incrementAndGet();
            return "ok";
        }

        @RateLimited(key = "search")
        public String anotherSearch() {
            return "ok";
        }

        @RateLimited(key = "detail")
        public String detail() {
            return "ok";
        }

        @GetMapping("/limited")
        @RateLimited
        public String defaultPolicy() {
            return "ok";
        }

        @RateLimited(key = "missing")
        public String missingPolicy() {
            calls.incrementAndGet();
            return "ok";
        }

        @RateLimited(key = "detail")
        public String failing() {
            throw new IllegalArgumentException("business failure");
        }

        public String unlimited() {
            return "ok";
        }
    }
}
