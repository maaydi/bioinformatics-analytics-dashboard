package com.bioinformatics.dashboard.job.uniprot.fileloader.processor.handler;

import com.bioinformatics.dashboard.job.uniprot.fileloader.processor.LineProcessor;
import com.bioinformatics.dashboard.job.uniprot.fileloader.processor.ProteinParsingContext;
import org.springframework.stereotype.Component;

@Component
public class OrganismSourceProcessor implements LineProcessor {
    @Override
    public String getPrefix() {
        return "OS";
    }

    @Override
    public void process(String line, ProteinParsingContext context) {
        context.getEntryBuilder().organismName(line.replace(".", ""));

    }
}
