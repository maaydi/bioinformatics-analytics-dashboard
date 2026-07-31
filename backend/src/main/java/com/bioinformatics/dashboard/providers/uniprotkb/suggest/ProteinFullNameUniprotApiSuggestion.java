package com.bioinformatics.dashboard.providers.uniprotkb.suggest;

import com.bioinformatics.dashboard.interfaces.suggest.SuggestionService;
import com.bioinformatics.dashboard.model.uniprot.dto.FullName;
import com.bioinformatics.dashboard.model.uniprot.dto.ProteinDescription;
import com.bioinformatics.dashboard.model.uniprot.dto.RecommendedName;
import com.bioinformatics.dashboard.providers.uniprotkb.AbstractUniprotKbProvider;
import com.bioinformatics.dashboard.providers.uniprotkb.dto.UniProtLightEntry;
import com.bioinformatics.dashboard.providers.uniprotkb.service.UniprotKbRestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * uniprot API suggestion provider for protein full names.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProteinFullNameUniprotApiSuggestion extends AbstractUniprotKbProvider implements SuggestionService {

    private final UniprotKbRestService uniprotKbRestService;

    @Override
    public String field() {
        return "ProteinFullName";
    }

    @Override
    public List<String> suggest(String query) {
        try {
            var result = uniprotKbRestService.searchAll("((protein_name:%s*))".formatted(query), 50);
            if (result.hasBody() && result.getBody() != null) {
                return result.getBody().results().stream()
                        .map(UniProtLightEntry::proteinDescription)
                        .map(ProteinDescription::recommendedName)
                        .map(RecommendedName::fullName)
                        .map(FullName::value)
                        .distinct()
                        .limit(10)
                        .toList();
            }

        } catch (Exception e) {
            log.warn("Error while searching for Protein full name with query {} : {}", query, e.getMessage());
        }
        return new ArrayList<>();
    }


}
