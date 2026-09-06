package com.bioinformatics.importservice.uniprot.fileloader.processor.handler;

import com.bioinformatics.importservice.resolver.ProteinAccessionResolver;
import com.bioinformatics.importservice.uniprot.fileloader.processor.LineProcessor;
import com.bioinformatics.importservice.uniprot.fileloader.processor.ProteinParsingContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccessionNumberProcessor implements LineProcessor {

    private final ProteinAccessionResolver accessionResolver;

    @Override
    public String getPrefix() {
        return "AC";
    }

    @Override
    public void process(String line, ProteinParsingContext context) {
        var acc = line.split(";")[0].trim();
        if (accessionResolver.alreadyExists(acc)) {
            context.setSkipEntry(true);
        }
        context.getEntryBuilder().accession(acc);
    }
}
