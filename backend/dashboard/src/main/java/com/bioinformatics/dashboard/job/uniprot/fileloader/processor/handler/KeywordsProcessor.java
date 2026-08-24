package com.bioinformatics.dashboard.job.uniprot.fileloader.processor.handler;

import com.bioinformatics.common.gene.entity.Keyword;
import com.bioinformatics.dashboard.job.uniprot.fileloader.processor.LineProcessor;
import com.bioinformatics.dashboard.job.uniprot.fileloader.processor.ProteinParsingContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class KeywordsProcessor implements LineProcessor {

    @Override
    public String getPrefix() {
        return "KW";
    }

    @Override
    public void process(String line, ProteinParsingContext context) {
        context.getKeywords().addAll(Arrays
                .stream(line.split(";"))
                .filter(e -> !e.isBlank())
                .map(e -> Keyword.builder().name(cleanStr(e)).build())
                .collect(Collectors.toSet()));
    }
}
