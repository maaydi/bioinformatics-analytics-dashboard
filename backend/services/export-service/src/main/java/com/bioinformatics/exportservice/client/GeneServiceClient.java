package com.bioinformatics.exportservice.client;

import com.bioinformatics.common.models.gene.GeneSearchRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

import static com.bioinformatics.shared.models.security.Constants.*;

@FeignClient(name = "dashboard")
public interface GeneServiceClient {
    @PostMapping("/api/genes/count")
    long countRequestRecords(@RequestBody GeneSearchRequest request, @RequestHeader(USER_ID_HEADER) String username, @RequestHeader(USER_ROLE_HEADER) List<String> roles, @RequestHeader(DATA_PROVIDER_HEADER) String dataProvider);
}
