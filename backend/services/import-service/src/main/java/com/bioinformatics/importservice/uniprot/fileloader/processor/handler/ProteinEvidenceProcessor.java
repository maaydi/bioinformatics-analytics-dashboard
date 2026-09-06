package com.bioinformatics.importservice.uniprot.fileloader.processor.handler;

import com.bioinformatics.importservice.uniprot.fileloader.processor.LineProcessor;
import com.bioinformatics.importservice.uniprot.fileloader.processor.ProteinParsingContext;
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
