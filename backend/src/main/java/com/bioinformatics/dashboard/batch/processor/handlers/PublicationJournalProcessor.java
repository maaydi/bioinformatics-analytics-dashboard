package com.bioinformatics.dashboard.batch.processor.handlers;

import com.bioinformatics.dashboard.batch.processor.LineProcessor;
import com.bioinformatics.dashboard.batch.processor.ProteinParsingContext;
import org.springframework.stereotype.Component;

@Component
public class PublicationJournalProcessor implements LineProcessor {

    @Override
    public String getPrefix() {
        return "RL";
    }

    @Override
    public void process(String line, ProteinParsingContext context) {
        var pubBuilders = context.getPubBuilders();
        if (!pubBuilders.isEmpty()) {
            var journal = pubBuilders.getLast().build().getJournal();
            pubBuilders.getLast().journal(String.join(journal, " ", line).trim());
        }
    }
}
