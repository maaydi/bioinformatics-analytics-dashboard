package com.bioinformatics.analyticsservice.controller;

import com.bioinformatics.analyticsservice.interfaces.FilteredAnalyticsService;
import com.bioinformatics.analyticsservice.models.compare.CompareRequestDto;
import com.bioinformatics.analyticsservice.models.compare.CompareResponseDto;
import com.bioinformatics.common.audit.annotation.Auditable;
import com.bioinformatics.common.audit.aspect.AuditAspect;
import com.bioinformatics.common.audit.service.AuditEventPublisher;
import com.bioinformatics.shared.models.audit.AuditAction;
import com.bioinformatics.shared.models.audit.AuditStatus;
import com.bioinformatics.shared.models.audit.AuditTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerAuditTest {
    @Mock
    private FilteredAnalyticsService service;
    @Mock
    private AuditEventPublisher publisher;
    private FilteredAnalyticsController controller;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        var factory = new AspectJProxyFactory(new FilteredAnalyticsController(service));
        factory.addAspect(new AuditAspect(publisher));
        controller = factory.getProxy();
    }

    @Test
    void explicitComparisonIsAuditedWithoutIncludingFiltersOrResults() {
        var request = mock(CompareRequestDto.class);
        var response = mock(CompareResponseDto.class);
        when(service.compare(request)).thenReturn(response);

        assertThat(controller.compare(request).getBody()).isSameAs(response);

        verify(publisher).publish(AuditAction.COMPARE_ANALYTICS, AuditTarget.COMPARE,
                "N/A", AuditStatus.SUCCESS, null);
    }

    @Test
    void comparisonFailuresAreAuditedAndPreserved() {
        var request = mock(CompareRequestDto.class);
        var original = new IllegalArgumentException("Invalid comparison");
        when(service.compare(request)).thenThrow(original);

        assertThatThrownBy(() -> controller.compare(request)).isSameAs(original);

        verify(publisher).publish(AuditAction.COMPARE_ANALYTICS, AuditTarget.COMPARE,
                "N/A", AuditStatus.FAILURE, null);
    }

    @Test
    void dashboardMetricsAndChartFanOutAreNotAudited() {
        assertThat(AnalyticsController.class.getDeclaredMethods())
                .filteredOn(method -> method.isAnnotationPresent(GetMapping.class))
                .hasSize(6)
                .allSatisfy(method -> assertThat(method.getAnnotation(Auditable.class)).isNull());
        assertThat(FilteredAnalyticsController.class.getDeclaredMethods())
                .filteredOn(method -> method.isAnnotationPresent(PostMapping.class) && !method.getName().equals("compare"))
                .hasSize(7)
                .allSatisfy(method -> assertThat(method.getAnnotation(Auditable.class)).isNull());
    }
}
