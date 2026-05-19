package com.bioinformatics.dashboard.batch.processor.handlers;

import com.bioinformatics.dashboard.batch.processor.LineProcessor;
import com.bioinformatics.dashboard.batch.processor.ProteinParsingContext;
import org.springframework.stereotype.Component;

@Component
public class ProteinEvidenceProcessor implements LineProcessor {

    @Override
    public String getPrefix() {
        return "PE";
    }

    @Override
    public void process(String line, ProteinParsingContext context) {
        context.getEntryBuilder().evidenceLevel(Short.parseShort(line.substring(0, 1)));

    }
}
