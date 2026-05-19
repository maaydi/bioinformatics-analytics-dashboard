package com.bioinformatics.dashboard.batch.processor;

import com.bioinformatics.dashboard.exception.MalformedUniprotFileException;
import com.bioinformatics.dashboard.gene.entity.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class ProteinParsingContext {

    private final ProteinEntry.ProteinEntryBuilder entryBuilder = ProteinEntry.builder();

    private final StringBuilder sequenceBuilder = new StringBuilder();
    private final List<String> lineageAccum = new ArrayList<>();
    private final List<String> orfNames = new ArrayList<>();
    private final List<String> synonyms = new ArrayList<>();
    private final Set<HostOrganism> hostOrganisms = new HashSet<>();
    private final List<ProteinPublication.ProteinPublicationBuilder> pubBuilders = new ArrayList<>();
    private final List<ProteinComment.ProteinCommentBuilder> commBuilders = new ArrayList<>();
    private final Set<CrossReference> crossRefs = new HashSet<>();
    private final List<Keyword> keywords = new ArrayList<>();
    private final List<ProteinFeature.ProteinFeatureBuilder> featureBuilders = new ArrayList<>();

    private boolean inSequence = false;
    private boolean skipEntry = false;


    public ProteinEntry build(List<Keyword> resolvedKeywords) {

        entryBuilder.sequence(sequenceBuilder.toString());
        entryBuilder.keywords(resolvedKeywords);
        if (!lineageAccum.isEmpty()) {
            entryBuilder.lineage(lineageAccum.toArray(new String[0]));
        }
        if (!orfNames.isEmpty()) {
            entryBuilder.geneOrfNames(orfNames.toArray(new String[0]));
        }
        if (!synonyms.isEmpty()) {
            entryBuilder.geneNameSynonyms(synonyms.toArray(new String[0]));
        }
        entryBuilder.hostOrganisms(hostOrganisms);
        entryBuilder.features(
                featureBuilders
                        .stream()
                        .map(ProteinFeature.ProteinFeatureBuilder::build)
                        .collect(Collectors.toSet())
        );

        var entry = entryBuilder.build();

        // Assert Bi-directional relation for JPA-cascaded children
        entry.getHostOrganisms().forEach(e -> e.setProtein(entry));
        entry.getFeatures().forEach(e -> e.setProtein(entry));

        // Populate transient collections (writer will persist these and set protein reference)
        entry.getCrossReferences().addAll(crossRefs);
        entry.getComments().addAll(commBuilders.stream()
                .map(ProteinComment.ProteinCommentBuilder::build)
                .collect(java.util.stream.Collectors.toSet()));
        entry.getPublications().addAll(pubBuilders.stream()
                .map(ProteinPublication.ProteinPublicationBuilder::build)
                .collect(java.util.stream.Collectors.toSet()));

        if (entry.getAccession() == null || entry.getEntryName() == null) {
            throw new MalformedUniprotFileException("Malformed Data: Missing Accession or Entry Name");
        }

        return entry;
    }

}
