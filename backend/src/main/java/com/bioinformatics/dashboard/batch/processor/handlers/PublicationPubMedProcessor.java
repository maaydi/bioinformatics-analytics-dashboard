package com.bioinformatics.dashboard.batch.processor.handlers;

import com.bioinformatics.dashboard.batch.processor.LineProcessor;
import com.bioinformatics.dashboard.batch.processor.ProteinParsingContext;
import org.springframework.stereotype.Component;

@Component
public class PublicationPubMedProcessor implements LineProcessor {

    @Override
    public String getPrefix() {
        return "RX";
    }

    @Override
    public void process(String line, ProteinParsingContext context) {
        var pubBuilders = context.getPubBuilders();
        if (!pubBuilders.isEmpty()) {
            pubBuilders.getLast().pubmedId(extractValue(line, "PubMed="));
            pubBuilders.getLast().doi(extractValue(line, "DOI="));
        }
    }
}
