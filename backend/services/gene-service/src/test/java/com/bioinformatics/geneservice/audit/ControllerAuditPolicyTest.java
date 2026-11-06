package com.bioinformatics.geneservice.audit;

import com.bioinformatics.common.audit.annotation.Auditable;
import com.bioinformatics.geneservice.controller.AutoCompleteController;
import com.bioinformatics.geneservice.controller.GeneController;
import com.bioinformatics.geneservice.savedfilter.controller.SavedFilterController;
import com.bioinformatics.shared.models.security.UserPrincipal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ControllerAuditPolicyTest {
    @Test
    void autocompleteAndRoutineReadsAreNotMisrepresentedAsBusinessMutations() throws Exception {
        assertThat(AutoCompleteController.class.getMethod("suggest", String.class, String.class)
                .getAnnotation(Auditable.class)).isNull();
        assertThat(SavedFilterController.class.getMethod("listSavedFilters", int.class, int.class, UserPrincipal.class)
                .getAnnotation(Auditable.class)).isNull();
        assertThat(GeneController.class.getDeclaredMethods())
                .filteredOn(method -> method.getName().equals("countRequestRows"))
                .hasSize(1)
                .allSatisfy(method -> assertThat(method.getAnnotation(Auditable.class)).isNull());
    }
}
