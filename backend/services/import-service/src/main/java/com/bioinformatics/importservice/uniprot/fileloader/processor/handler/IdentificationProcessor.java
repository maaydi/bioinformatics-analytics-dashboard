package com.bioinformatics.importservice.uniprot.fileloader.processor.handler;

import com.bioinformatics.importservice.uniprot.fileloader.processor.LineProcessor;
import com.bioinformatics.importservice.uniprot.fileloader.processor.ProteinParsingContext;
import org.springframework.stereotype.Component;

@Component
public class IdentificationProcessor implements LineProcessor {
    @Override
    public String getPrefix() {
        return "ID";
    }

    @Override
    public void process(String line, ProteinParsingContext context) {
        var idParts = line.split("\\s+");
        context.getEntryBuilder().entryName(idParts[0]);
        context.getEntryBuilder().reviewed("Reviewed;".equalsIgnoreCase(idParts[1]));
        if (idParts.length >= 3) {
            context.getEntryBuilder().length(Integer.parseInt(idParts[2]));
        }
    }
}
