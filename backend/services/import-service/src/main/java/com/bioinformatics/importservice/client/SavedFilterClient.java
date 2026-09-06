package com.bioinformatics.importservice.client;

import com.bioinformatics.common.models.filter.SavedFilterDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

import static com.bioinformatics.shared.models.security.Constants.USER_ID_HEADER;
import static com.bioinformatics.shared.models.security.Constants.USER_ROLE_HEADER;

@FeignClient(name = "dashboard")
public interface SavedFilterClient {
    @GetMapping("/api/saved-filters/{id}")
    ResponseEntity<SavedFilterDto> getSavedFilterById(@PathVariable Long id, @RequestHeader(USER_ID_HEADER) String username, @RequestHeader(USER_ROLE_HEADER) List<String> roles);
}
