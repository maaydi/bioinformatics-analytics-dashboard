package com.bioinformatics.exportservice.client;

import com.bioinformatics.common.models.gene.GeneSearchRequest;
import com.bioinformatics.shared.models.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeneService {
    private final GeneServiceClient client;

    public long count(GeneSearchRequest request, UserPrincipal user) {
        return client.countRequestRecords(request, user.id(), user.roles());

    }
}
