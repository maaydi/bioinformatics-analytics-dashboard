package com.bioinformatics.dashboard.job.uniprot.fileloader.processor.handler;

import com.bioinformatics.common.gene.entity.ProteinPublication;
import com.bioinformatics.dashboard.job.uniprot.fileloader.processor.LineProcessor;
import com.bioinformatics.dashboard.job.uniprot.fileloader.processor.ProteinParsingContext;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PublicationReferenceNumberProcessor implements LineProcessor {
    private static final Pattern REF_NUMBER_PATTERN = Pattern.compile("\\[(\\d+)]");

    @Override
    public String getPrefix() {
        return "RN";
    }

    @Override
    public void process(String line, ProteinParsingContext context) {
        var pubBuilder = ProteinPublication.builder();
        var refMatcher = REF_NUMBER_PATTERN.matcher(line);
        if (refMatcher.find()) {
            pubBuilder.refNumber(Short.parseShort(refMatcher.group(1)));
        }
        pubBuilder.title("");
        pubBuilder.authors("");
        pubBuilder.journal("");
        context.getPubBuilders().add(pubBuilder);
    }
}
