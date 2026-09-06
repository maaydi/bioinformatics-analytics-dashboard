package com.bioinformatics.dashboard.providers.uniprotkb.suggest;

import com.bioinformatics.common.providers.uniprotkb.dto.UniProtLightEntry;
import com.bioinformatics.common.providers.uniprotkb.service.UniprotKbRestService;
import com.bioinformatics.dashboard.interfaces.suggest.SuggestionService;
import com.bioinformatics.dashboard.providers.uniprotkb.AbstractUniprotKbProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.bioinformatics.common.uniprot.UniprotMapperUtils.INACTIVE_ENTRY_TYPE;

/**
 * uniprot API suggestion provider for protein entry names.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EntryNameUniprotApiSuggestion extends AbstractUniprotKbProvider implements SuggestionService {

    private final UniprotKbRestService uniprotKbRestService;

    @Override
    public String field() {
        return "EntryName";
    }

    @Override
    public List<String> suggest(String query) {
        try {
            var result = uniprotKbRestService.searchAll("((id:%s*))".formatted(query), 50);
            if (result.hasBody() && result.getBody() != null) {
                return result.getBody().results().stream()
                        .filter(e -> !INACTIVE_ENTRY_TYPE.equalsIgnoreCase(e.entryType()))
                        .map(UniProtLightEntry::uniProtkbId)
                        .distinct()
                        .limit(10)
                        .toList();
            }

        } catch (Exception e) {
            log.warn("Error while searching for Entry name with query {} : {}", query, e.getMessage());
        }
        return new ArrayList<>();
    }


}
