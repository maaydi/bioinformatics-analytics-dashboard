package com.bioinformatics.dashboard.batch;

import com.bioinformatics.dashboard.exception.MalformedUniprotFileException;
import com.bioinformatics.dashboard.gene.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ProteinEntryItemProcessor implements ItemProcessor<String, ProteinEntry> {

    private static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("dd-MMM-yyyy")
            .toFormatter(Locale.ENGLISH);

    private static final Pattern OX_TAXID_PATTERN = Pattern.compile("NCBI_TaxID=(\\d+)");
    private static final Pattern SQ_MW_PATTERN = Pattern.compile("(\\d+)\\s+MW");
    private static final Pattern SQ_CRC64_PATTERN = Pattern.compile("([A-F0-9]+)\\s+CRC64");
    private static final Pattern VERSION_PATTERN = Pattern.compile("version (\\d+)");
    private static final Pattern REF_NUMBER_PATTERN = Pattern.compile("\\[(\\d+)]");
    private static final Pattern FEATURE_PATTERN = Pattern.compile("([A-Z_]+)\\s+(\\d+)\\.\\.(\\d+)");

    @Override
    public @Nullable ProteinEntry process(@NonNull String item) {
        if (item.trim().isEmpty())
            return null;
        var entryBuilder = ProteinEntry.builder();

        var sequenceBuilder = new StringBuilder();
        var lineageAccum = new ArrayList<String>();
        var orfNames = new ArrayList<String>();
        var synonyms = new ArrayList<String>();
        var hostOrganisms = new HashSet<HostOrganism>();
        var pubBuilders = new ArrayList<ProteinPublication.ProteinPublicationBuilder>();
        var commBuilders = new ArrayList<ProteinComment.ProteinCommentBuilder>();
        var crossRefs = new HashSet<CrossReference>();
        var keywords = new ArrayList<Keyword>();
        var featureBuilders = new ArrayList<ProteinFeature.ProteinFeatureBuilder>();

        var inSequence = false;
        var lines = item.split("\n");
        for (var line : lines) {
            if (line.length() < 5)
                continue;
            var prefix = line.substring(0, 2);
            var data = line.substring(5).trim();
            if (inSequence) {
                if (!line.startsWith("//")) {
                    sequenceBuilder.append(line.replaceAll("\\s+", ""));
                }
                continue;

            }
            switch (prefix) {
                // 1. Identification
                case "ID":
                    var idParts = data.split("\\s+");
                    entryBuilder.entryName(idParts[0]);
                    entryBuilder.reviewed("Reviewed;".equalsIgnoreCase(idParts[1]));
                    if (idParts.length >= 3) {
                        entryBuilder.length(Integer.parseInt(idParts[2]));
                    }
                    break;
                // 2. Accession Number
                case "AC":
                    entryBuilder.accession(data.split(";")[0].trim());
                    break;
                // 3. Dates
                case "DT":
                    var dateStr = data.split(",")[0].trim();
                    var date = LocalDate.parse(dateStr, DATE_FORMATTER);
                    if (data.contains("integrated")) {
                        entryBuilder.integratedDate(date);
                    } else if (data.contains("sequence version")) {
                        entryBuilder.sequenceDate(date);
                        var m = VERSION_PATTERN.matcher(data);
                        if (m.find()) {
                            entryBuilder.sequenceVersion(Short.parseShort(m.group(1)));
                        }
                    } else if (data.contains("entry version")) {
                        entryBuilder.updatedDate(date);
                        var m = VERSION_PATTERN.matcher(data);
                        if (m.find()) {
                            entryBuilder.entryVersion(Short.parseShort(m.group(1)));
                        }
                    }
                    break;
                // 4. Description / Protein name
                case "DE":
                    if (data.startsWith("RecName: Full=")) {
                        entryBuilder.proteinFullName(extractValue(data, "Full="));
                    } else if (data.startsWith("RecName: Short=")) {
                        entryBuilder.proteinShortName(extractValue(data, "Short="));
                    } else if (data.contains("EC=")) {
                        entryBuilder.proteinEcNumber(extractValue(data, "EC="));
                    }
                    break;
                // 5. Gene name
                case "GN":
                    if (data.startsWith("Name=")) {
                        entryBuilder.geneNamePrimary(extractValue(data, "Name="));
                    } else if (data.contains("Synonyms=")) {
                        synonyms.add(extractValue(data, "Synonyms="));
                    } else if (data.contains("ORFNames=")) {
                        orfNames.add(extractValue(data, "ORFNames="));
                    }
                    break;
                // 6. Organism Source
                case "OS":
                    entryBuilder.organismName(data.replace(".", ""));
                    break;
                // 7. Taxonomy Classification
                case "OC":
                    var ocParts = data.split(";");
                    for (var part : ocParts) {
                        if (!part.trim().isEmpty() && !part.trim().equals(".")) {
                            lineageAccum.add(part.trim());
                        }
                    }
                    break;
                // 8. Taxonomy ID
                case "OX":
                    var oxMatcher = OX_TAXID_PATTERN.matcher(data);
                    if (oxMatcher.find()) {
                        entryBuilder.taxid(Integer.parseInt(oxMatcher.group(1)));
                    }
                    break;
                // 9. Host Organisms
                case "OH":
                    var hostBuilder = HostOrganism.builder();
                    var oxMatch = OX_TAXID_PATTERN.matcher(data);
                    if (oxMatch.find()) {
                        hostBuilder.taxid(Integer.parseInt(oxMatch.group(1)));
                    }
                    hostBuilder.name(data.split(";")[1].trim());
                    hostOrganisms.add(hostBuilder.build());
                    break;
                // 10. References / Publication
                case "RN":
                    var pubBuilder = ProteinPublication.builder();
                    var refMatcher = REF_NUMBER_PATTERN.matcher(data);
                    if (refMatcher.find()) {
                        pubBuilder.refNumber(Short.parseShort(refMatcher.group(1)));
                    }
                    pubBuilder.title("");
                    pubBuilder.authors("");
                    pubBuilder.journal("");
                    pubBuilders.add(pubBuilder);
                    break;
                case "RP":
                    // ignore what was studied in domain model
                    break;
                case "RX":
                    pubBuilders.getLast().pubmedId(extractValue(data, "PubMed="));
                    pubBuilders.getLast().doi(extractValue(data, "DOI="));
                    break;
                case "RA":
                    var author = pubBuilders.getLast().build().getAuthors();
                    pubBuilders.getLast().authors(String.join(author, " ", data.split(";")[0]).trim());
                    break;
                case "RT":
                    var title = pubBuilders.getLast().build().getTitle();
                    pubBuilders.getLast().title(String.join(title, " ", data.split(";")[0]).trim());
                    break;
                case "RL":
                    var journal = pubBuilders.getLast().build().getJournal();
                    pubBuilders.getLast().journal(String.join(journal, " ", data).trim());
                    break;
                // 11. Comments
                case "CC":
                    if (data.matches("-+")
                            || data.contains("Copyrighted by the UniProt Consortium")
                            || data.contains("Distributed under the Creative Commons Attribution")) {
                        // skip copyright and distributed lines
                        break;
                    }
                    if (data.contains("-!-")) {
                        // add new instance with comment type and text
                        var comBuilder = ProteinComment.builder();
                        var l = data.split("-!-")[1].split(":");
                        comBuilder.commentType(l[0].trim());
                        comBuilder.text(l[1].trim());
                        commBuilders.add(comBuilder);
                        break;
                    }
                    // same comment text in multiple lines
                    var c = commBuilders.getLast().build().getText();
                    commBuilders.getLast().text(String.join(c, " ", data));
                    break;
                // 12. Cross-References
                case "DR":
                    var items = data.split(";");
                    if (items.length >= 2) {
                        var cross = CrossReference.builder()
                                .source(cleanStr(items[0]))
                                .identifier(cleanStr(items[1]))
                                .secondaryId(cleanStr(items.length > 2 ? items[2] : ""))
                                .tertiaryInfo(cleanStr(items.length > 3 ? items[3] : ""))
                                .build();
                        crossRefs.add(cross);
                    }
                    break;
                // 13. Protein evidence
                case "PE":
                    entryBuilder.evidenceLevel(Short.parseShort(data.substring(0, 1)));
                    break;
                // 14. Keywords
                case "KW":
                    keywords.addAll(Arrays
                            .stream(data.split(";"))
                            .filter(e -> !e.isBlank())
                            .map(e -> Keyword.builder().name(cleanStr(e)).build())
                            .collect(Collectors.toSet()));
                    break;
                // 15. Feature table
                case "FT":
                    if (!data.contains("/")) {
                        var mt = FEATURE_PATTERN.matcher(data);
                        if (mt.find()) {
                            var ft = ProteinFeature.builder()
                                    .featureType(mt.group(1))
                                    .startPos(Integer.parseInt(mt.group(2)))
                                    .endPos(Integer.parseInt(mt.group(3)));
                            featureBuilders.add(ft);
                        }
                    } else {
                        if (data.startsWith("/id")) {
                            featureBuilders.getLast().featureId(data.split("=")[1]);
                        } else if (data.startsWith("/note")) {
                            featureBuilders.getLast().note(data.split("=")[1]);
                        } else if (data.startsWith("/evidence")) {
                            featureBuilders.getLast().evidence(data.split("=")[1]);
                        }
                    }
                    break;
                // 16. Sequence Header
                case "SQ":
                    var mwMatcher = SQ_MW_PATTERN.matcher(data);
                    if (mwMatcher.find()) {
                        entryBuilder.molecularWeight(Integer.parseInt(mwMatcher.group(1)));
                    }
                    var crcMatcher = SQ_CRC64_PATTERN.matcher(data);
                    if (crcMatcher.find()) {
                        entryBuilder.sequenceChecksum(crcMatcher.group(1));
                    }
                    inSequence = true;
                    break;
            }
        }

        entryBuilder.sequence(sequenceBuilder.toString());

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
        entryBuilder.publications(pubBuilders
                .stream()
                .map(ProteinPublication.ProteinPublicationBuilder::build)
                .collect(Collectors.toSet()));
        entryBuilder.comments(commBuilders
                .stream()
                .map(ProteinComment.ProteinCommentBuilder::build)
                .collect(Collectors.toSet()));
        entryBuilder.crossReferences(crossRefs);
        entryBuilder.keywords(keywords);
        entryBuilder.features(
                featureBuilders
                        .stream()
                        .map(ProteinFeature.ProteinFeatureBuilder::build)
                        .collect(Collectors.toSet())
        );
        var entry = entryBuilder.build();

        if (entry.getAccession() == null || entry.getEntryName() == null) {
            throw new MalformedUniprotFileException("Malformed Data: Missing Accession or Entry Name");
        }

        return entry;
    }

    /**
     * Helper to extract values from String <KEY=VALUE;>
     * "Full=Putative transcription factor;" -> "Putative transcription factor"
     */
    private String extractValue(String line, String key) {
        int start = line.indexOf(key) + key.length();
        int end = line.indexOf(";", start);
        if (end == -1)
            end = line.length();
        return line.substring(start, end).trim();
    }

    /**
     * Remove extra space and end "." in a value
     *
     */
    private String cleanStr(final String value) {
        var v = value.trim();
        if (v.endsWith(".")) {
            return v.substring(0, v.length() - 1);
        }
        return v;
    }


}