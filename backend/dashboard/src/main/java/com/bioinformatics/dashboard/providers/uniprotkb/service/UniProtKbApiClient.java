package com.bioinformatics.dashboard.providers.uniprotkb.service;

import com.bioinformatics.common.models.gene.GeneSearchRequest;
import com.bioinformatics.dashboard.interfaces.UniProtApiClient;
import com.bioinformatics.dashboard.model.uniprot.UniProtApiPage;
import com.bioinformatics.dashboard.model.uniprot.dto.UniProtEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Production implementation of {@link UniProtApiClient} that delegates to
 * {@link UniprotKbRestService} and drives cursor-based pagination via the
 * {@code Link} response header.
 *
 * <h3>Link header format</h3>
 * <pre>
 *   &lt;<a href="https://rest.uniprot.org/uniprotkb/search?...&amp;cursor=XYZ&amp;size=N&gt">...</a>;; rel="next"
 * </pre>
 * The cursor value is extracted with a regex and threaded into the next call.
 *
 * <h3>Progress logging</h3>
 * The {@code x-total-results} header is read once on the first response and
 * logged so operators can monitor batch progress as a percentage.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UniProtKbApiClient implements UniProtApiClient {

    /**
     * Matches the {@code cursor} query parameter inside the angle-bracket URL of a
     * {@code Link} header, e.g.
     * {@code <https://…?cursor=abc123&size=5>; rel="next"}.
     */
    private static final Pattern CURSOR_PATTERN =
            Pattern.compile("[?&]cursor=([^&>]+)");

    private static final String LINK_HEADER = "link";
    private static final String TOTAL_RESULTS_HEADER = "x-total-results";

    private final UniprotKbRestService restService;

    /**
     * Total number of entries signaled by the API on the first page.
     * Used only for progress logging — never for business logic.
     */
    private long totalResults = -1;

    @Override
    public UniProtApiPage fetchPage(GeneSearchRequest request, String cursor) {
        log.debug("Fetching page for request {} with cursor {}", request, cursor);
        var response = restService.searchAll(request, cursor);

        captureTotal(response);

        var body = response.getBody();
        var entries = body != null ? body.results() : new ArrayList<UniProtEntry>();

        var nextCursor = extractNextCursor(response);
        if (nextCursor == null) {
            log.debug("Last page received — {} entries fetched on this page", entries.size());
            return UniProtApiPage.lastPage(entries, totalResults);
        }
        log.debug("Page fetched ({} entries). Total reported by API: {}.", entries.size(), totalResults);
        return UniProtApiPage.nextPage(entries, nextCursor, totalResults);
    }

    /**
     * Reads {@code x-total-results} from the first response only and stores it
     * for progress-percentage logging.
     */
    private void captureTotal(ResponseEntity<?> response) {
        var raw = response.getHeaders().getFirst(TOTAL_RESULTS_HEADER);
        if (raw != null) {
            try {
                totalResults = Long.parseLong(raw.trim());
                log.debug("UniProt total results: {}", totalResults);
            } catch (NumberFormatException e) {
                log.warn("Could not parse {} header value '{}': {}",
                        TOTAL_RESULTS_HEADER, raw, e.getMessage());
            }
        }
    }

    /**
     * Extracts the cursor from the {@code Link: <…?cursor=XYZ…>; rel="next"} header.
     *
     * @return the cursor string, or {@code null} if no {@code rel="next"} link is present
     */
    private String extractNextCursor(ResponseEntity<?> response) {
        log.debug("Extract Next cursor from response 'link' header: {}",
                response.getHeaders().getFirst(LINK_HEADER));
        var linkHeader = response.getHeaders().getFirst(LINK_HEADER);
        if (linkHeader == null || linkHeader.isBlank()) {
            return null;
        }

        // Only parse the segment whose rel attribute is "next"
        for (var segment : linkHeader.split(",")) {
            if (segment.contains("rel=\"next\"")) {
                var matcher = CURSOR_PATTERN.matcher(segment);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        }
        return null;
    }

}

