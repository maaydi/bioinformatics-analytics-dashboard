package com.bioinformatics.dashboard.batch.processor.handlers;

import com.bioinformatics.dashboard.batch.processor.LineProcessor;
import com.bioinformatics.dashboard.batch.processor.ProteinParsingContext;
import com.bioinformatics.dashboard.gene.entity.Keyword;
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
