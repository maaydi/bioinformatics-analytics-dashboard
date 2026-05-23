package com.bioinformatics.dashboard.batch.processor.handlers;

import com.bioinformatics.dashboard.batch.processor.LineProcessor;
import com.bioinformatics.dashboard.batch.processor.ProteinParsingContext;
import org.springframework.stereotype.Component;

@Component
public class PublicationAuthorProcessor implements LineProcessor {

    @Override
    public String getPrefix() {
        return "RA";
    }

    @Override
    public void process(String line, ProteinParsingContext context) {
        var pubBuilders = context.getPubBuilders();
        if (!pubBuilders.isEmpty()) {
            var author = pubBuilders.getLast().build().getAuthors();
            pubBuilders.getLast().authors(String.join(author, " ", line.split(";")[0]).trim());
        }
    }
}
