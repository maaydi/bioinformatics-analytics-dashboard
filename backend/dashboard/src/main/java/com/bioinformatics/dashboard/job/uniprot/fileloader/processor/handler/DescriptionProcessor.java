package com.bioinformatics.dashboard.job.uniprot.fileloader.processor.handler;

import com.bioinformatics.dashboard.job.uniprot.fileloader.processor.LineProcessor;
import com.bioinformatics.dashboard.job.uniprot.fileloader.processor.ProteinParsingContext;
import org.springframework.stereotype.Component;

@Component
public class DescriptionProcessor implements LineProcessor {
    @Override
    public String getPrefix() {
        return "DE";
    }

    @Override
    public void process(String line, ProteinParsingContext context) {
        var entryBuilder = context.getEntryBuilder();
        if (line.startsWith("RecName: Full=")) {
            entryBuilder.proteinFullName(extractValue(line, "Full="));
        } else if (line.startsWith("RecName: Short=")) {
            entryBuilder.proteinShortName(extractValue(line, "Short="));
        } else if (line.contains("EC=")) {
            entryBuilder.proteinEcNumber(extractValue(line, "EC="));
        }
    }
}
