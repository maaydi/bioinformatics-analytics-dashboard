package com.bioinformatics.dashboard.batch.processor.handlers;

import com.bioinformatics.dashboard.batch.processor.LineProcessor;
import com.bioinformatics.dashboard.batch.processor.ProteinParsingContext;
import com.bioinformatics.dashboard.batch.processor.resolver.ProteinAccessionResolver;
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
