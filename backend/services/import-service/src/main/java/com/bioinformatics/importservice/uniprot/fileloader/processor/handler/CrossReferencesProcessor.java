package com.bioinformatics.importservice.uniprot.fileloader.processor.handler;

import com.bioinformatics.common.gene.entity.CrossReference;
import com.bioinformatics.importservice.uniprot.fileloader.processor.LineProcessor;
import com.bioinformatics.importservice.uniprot.fileloader.processor.ProteinParsingContext;
import org.springframework.stereotype.Component;

@Component
public class CrossReferencesProcessor implements LineProcessor {

    @Override
    public String getPrefix() {
        return "DR";
    }

    @Override
    public void process(String line, ProteinParsingContext context) {
        var items = line.split(";");
        if (items.length >= 2) {
            var cross = CrossReference.builder()
                    .source(cleanStr(items[0]))
                    .identifier(cleanStr(items[1]))
                    .secondaryId(cleanStr(items.length > 2 ? items[2] : ""))
                    .tertiaryInfo(cleanStr(items.length > 3 ? items[3] : ""))
                    .build();
            context.getCrossRefs().add(cross);
        }
    }
}
