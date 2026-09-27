package com.bioinformatics.exportservice.client;

import com.bioinformatics.common.models.gene.GeneSearchRequest;
import com.bioinformatics.shared.models.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Adapter for gene count operations performed against the dashboard gene service.
 *
 * <p>This client encapsulates the request metadata required by the downstream service,
 * including the authenticated user identity and provider context.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeneService {
    private final GeneServiceClient client;

    /**
     * Counts the number of genes matching the supplied filter for the current user.
     *
     * @param request gene search criteria to evaluate
     * @param user    authenticated caller context used to apply provider and authorization rules
     * @return number of matching genes
     */
    public long count(GeneSearchRequest request, UserPrincipal user) {
        log.debug("[EXPORT][GENE_CLIENT] Counting matching genes for userId={} provider={} request={}",
                user.id(), user.dataProvider(), request);

        long total = client.countRequestRecords(request, user.id(), user.roles(), user.dataProvider());

        log.info("[EXPORT][GENE_CLIENT] Gene count resolved for userId={} provider={} total={}",
                user.id(), user.dataProvider(), total);
        return total;
    }
}
