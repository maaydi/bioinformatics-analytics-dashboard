package com.bioinformatics.dashboard.providers.uniprotkb.suggest;

import com.bioinformatics.common.providers.uniprotkb.dto.GeneLight;
import com.bioinformatics.common.providers.uniprotkb.dto.UniProtLightEntry;
import com.bioinformatics.common.providers.uniprotkb.service.UniprotKbRestService;
import com.bioinformatics.common.uniprot.dto.GeneName;
import com.bioinformatics.dashboard.interfaces.suggest.SuggestionService;
import com.bioinformatics.dashboard.providers.uniprotkb.AbstractUniprotKbProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.bioinformatics.common.uniprot.UniprotMapperUtils.INACTIVE_ENTRY_TYPE;

/**
 * uniprot API suggestion provider for primary gene names.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GeneNamePrimaryUniprotApiSuggestion extends AbstractUniprotKbProvider implements SuggestionService {


    private final UniprotKbRestService uniprotKbRestService;

    @Override
    public String field() {
        return "GeneNamePrimary";
    }

    @Override
    public List<String> suggest(String query) {
        try {
            var result = uniprotKbRestService.searchAll("((gene:%s*))".formatted(query), 50);
            if (result.hasBody() && result.getBody() != null) {
                return result.getBody().results().stream()
                        .filter(e -> !INACTIVE_ENTRY_TYPE.equalsIgnoreCase(e.entryType()))
                        .map(UniProtLightEntry::genes)
                        .filter(e -> e != null && !e.isEmpty())
                        .map(List::getFirst)
                        .map(GeneLight::geneName)
                        .filter(Objects::nonNull)
                        .map(GeneName::value)
                        .distinct()
                        .limit(10)
                        .toList();
            }

        } catch (Exception e) {
            log.warn("Error while searching for gene primary name with query {} : {}", query, e.getMessage());
        }
        return new ArrayList<>();
    }


}
