package com.bioinformatics.dashboard.batch.processor.handlers;

import com.bioinformatics.dashboard.batch.processor.LineProcessor;
import com.bioinformatics.dashboard.batch.processor.ProteinParsingContext;
import com.bioinformatics.dashboard.providers.postgres.gene.entity.ProteinComment;
import org.springframework.stereotype.Component;

@Component
public class CommentsProcessor implements LineProcessor {

    @Override
    public String getPrefix() {
        return "CC";
    }

    @Override
    public void process(String line, ProteinParsingContext context) {
        if (line.matches("-+")
                || line.contains("Copyrighted by the UniProt Consortium")
                || line.contains("Distributed under the Creative Commons Attribution")) {
            // skip copyright and distributed lines
            return;
        }
        var commBuilders = context.getCommBuilders();
        if (line.contains("-!-")) {
            // add new instance with comment type and text
            var comBuilder = ProteinComment.builder();
            var l = line.split("-!-")[1].split(":");
            comBuilder.commentType(l[0].trim());
            comBuilder.text(l.length > 1 ? l[1].trim().replace(";", "") : "");
            commBuilders.add(comBuilder);
            return;
        }
        // same comment text in multiple lines
        if (!commBuilders.isEmpty()) {
            var c = commBuilders.getLast().build().getText();
            commBuilders.getLast().text(String.join(c, " ", line));
        }
    }
}
