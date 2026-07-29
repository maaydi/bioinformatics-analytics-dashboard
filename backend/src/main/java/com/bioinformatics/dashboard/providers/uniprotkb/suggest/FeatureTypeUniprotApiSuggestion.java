package com.bioinformatics.dashboard.providers.uniprotkb.suggest;

import com.bioinformatics.dashboard.interfaces.suggest.SuggestionService;
import com.bioinformatics.dashboard.providers.uniprotkb.AbstractUniprotKbProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * uniprot API suggestion provider for protein feature types.
 */
@Component
@RequiredArgsConstructor
public class FeatureTypeUniprotApiSuggestion extends AbstractUniprotKbProvider implements SuggestionService {


    @Override
    public String field() {
        return "FeatureType";
    }

    @Override
    public List<String> suggest(String query) {
        // TODO extract it from https://rest.uniprot.org/configure/uniprotkb/search-fields
        // TODO load at startup component and use them as map
        // feature type is defined by label where siblings contains term start with "ft_"
        /**
         *
         * {
         *         "id": "glycosylation_ft",
         *         "label": "Glycosylation [FT]", // feature type
         *         "itemType": "sibling_group",
         *         "siblings": [
         *           {
         *             "id": "ft_carbohyd", // correspond in search normalizedType
         *             "itemType": "single",
         *
         * public static Optional<String> featureType(String type) {
         *         if (!StringUtils.hasText(type)) return Optional.empty();
         *         var normalizedType = type.trim().toLowerCase().replace(" ", "_");
         *         return Optional.of("ft_" + normalizedType + ":*");
         *     }
         * */
        throw new UnsupportedOperationException("FeatureType suggestion is not supported yet.");
    }


}
