package com.bioinformatics.dashboard.batch.processor.handlers;

import com.bioinformatics.dashboard.batch.processor.LineProcessor;
import com.bioinformatics.dashboard.batch.processor.ProteinParsingContext;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PublicationTitleProcessor implements LineProcessor {
    private static final Pattern REF_NUMBER_PATTERN = Pattern.compile("\\[(\\d+)]");

    @Override
    public String getPrefix() {
        return "RT";
    }

    @Override
    public void process(String line, ProteinParsingContext context) {
        var pubBuilders = context.getPubBuilders();
        if (!pubBuilders.isEmpty()) {
            var title = pubBuilders.getLast().build().getTitle();
            pubBuilders.getLast().title(String.join(title, " ", line.split(";")[0]).trim());
        }
    }
}
