package com.bioinformatics.dashboard.batch;

import com.bioinformatics.dashboard.exception.MalformedUniprotFileException;
import com.bioinformatics.dashboard.gene.entity.ProteinEntry;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

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

    @Override
    public @Nullable ProteinEntry process(@NonNull String item) {
        if (item.trim().isEmpty())
            return null;
        var entryBuilder = ProteinEntry.builder();

        var sequenceBuilder = new StringBuilder();
        var lineageAccum = new ArrayList<String>();
        var orfNames = new ArrayList<String>();
        var synonyms = new ArrayList<String>();

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
                case "ID":
                    var idParts = data.split("\\s+");
                    entryBuilder.entryName(idParts[0]);
                    entryBuilder.reviewed("Reviewed;".equalsIgnoreCase(idParts[1]));
                    if (idParts.length >= 3) {
                        entryBuilder.length(Integer.parseInt(idParts[2]));
                    }
                    break;
                case "AC":
                    entryBuilder.accession(data.split(";")[0].trim());
                    break;
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
                case "DE":
                    if (data.startsWith("RecName: Full=")) {
                        entryBuilder.proteinFullName(extractValue(data, "Full="));
                    } else if (data.startsWith("RecName: Short=")) {
                        entryBuilder.proteinShortName(extractValue(data, "Short="));
                    } else if (data.contains("EC=")) {
                        entryBuilder.proteinEcNumber(extractValue(data, "EC="));
                    }
                    break;

                case "GN":
                    if (data.startsWith("Name=")) {
                        entryBuilder.geneNamePrimary(extractValue(data, "Name="));
                    } else if (data.contains("Synonyms=")) {
                        synonyms.add(extractValue(data, "Synonyms="));
                    } else if (data.contains("ORFNames=")) {
                        orfNames.add(extractValue(data, "ORFNames="));
                    }
                    break;

                case "OS":
                    entryBuilder.organismName(data.replace(".", ""));
                    break;

                case "OC":
                    var ocParts = data.split(";");
                    for (var part : ocParts) {
                        if (!part.trim().isEmpty() && !part.trim().equals(".")) {
                            lineageAccum.add(part.trim());
                        }
                    }
                    break;

                case "OX":
                    var oxMatcher = OX_TAXID_PATTERN.matcher(data);
                    if (oxMatcher.find()) {
                        entryBuilder.taxid(Integer.parseInt(oxMatcher.group(1)));
                    }
                    break;

                case "PE":
                    entryBuilder.evidenceLevel(Short.parseShort(data.substring(0, 1)));
                    break;

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
}