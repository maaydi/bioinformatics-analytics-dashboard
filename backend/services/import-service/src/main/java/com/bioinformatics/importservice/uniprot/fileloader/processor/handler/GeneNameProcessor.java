package com.bioinformatics.importservice.uniprot.fileloader.processor.handler;

import com.bioinformatics.importservice.uniprot.fileloader.processor.LineProcessor;
import com.bioinformatics.importservice.uniprot.fileloader.processor.ProteinParsingContext;
import org.springframework.stereotype.Component;

@Component
public class GeneNameProcessor implements LineProcessor {
    @Override
    public String getPrefix() {
        return "GN";
    }

    @Override
    public void process(String line, ProteinParsingContext context) {
        if (line.startsWith("Name=")) {
            context.getEntryBuilder().geneNamePrimary(extractValue(line, "Name="));
        } else if (line.contains("Synonyms=")) {
            context.getSynonyms().add(extractValue(line, "Synonyms="));
        } else if (line.contains("ORFNames=")) {
            context.getOrfNames().add(extractValue(line, "ORFNames="));
        }
    }
}
