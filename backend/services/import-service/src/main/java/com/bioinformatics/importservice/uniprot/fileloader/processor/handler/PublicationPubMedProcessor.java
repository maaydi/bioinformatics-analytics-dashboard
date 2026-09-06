package com.bioinformatics.importservice.uniprot.fileloader.processor.handler;

import com.bioinformatics.importservice.uniprot.fileloader.processor.LineProcessor;
import com.bioinformatics.importservice.uniprot.fileloader.processor.ProteinParsingContext;
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
