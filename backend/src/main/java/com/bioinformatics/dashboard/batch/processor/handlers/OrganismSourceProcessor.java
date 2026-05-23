package com.bioinformatics.dashboard.batch.processor.handlers;

import com.bioinformatics.dashboard.batch.processor.LineProcessor;
import com.bioinformatics.dashboard.batch.processor.ProteinParsingContext;
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
