package com.bioinformatics.importservice.uniprot.fileloader.processor.handler;

import com.bioinformatics.importservice.uniprot.fileloader.processor.LineProcessor;
import com.bioinformatics.importservice.uniprot.fileloader.processor.ProteinParsingContext;
import org.springframework.stereotype.Component;

@Component
public class TaxonomyClassificationProcessor implements LineProcessor {
    @Override
    public String getPrefix() {
        return "OC";
    }

    @Override
    public void process(String line, ProteinParsingContext context) {
        var ocParts = line.split(";");
        for (var part : ocParts) {
            if (!part.trim().isEmpty() && !part.trim().equals(".")) {
                context.getLineageAccum().add(part.trim());
            }
        }
    }
}
