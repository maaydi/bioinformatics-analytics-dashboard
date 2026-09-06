package com.bioinformatics.importservice.uniprot.fileloader.processor.handler;

import com.bioinformatics.importservice.uniprot.fileloader.processor.LineProcessor;
import com.bioinformatics.importservice.uniprot.fileloader.processor.ProteinParsingContext;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class TaxonomyIdProcessor implements LineProcessor {
    private static final Pattern OX_TAXID_PATTERN = Pattern.compile("NCBI_TaxID=(\\d+)");

    @Override
    public String getPrefix() {
        return "OX";
    }

    @Override
    public void process(String line, ProteinParsingContext context) {
        var oxMatcher = OX_TAXID_PATTERN.matcher(line);
        if (oxMatcher.find()) {
            context.getEntryBuilder().taxid(Integer.parseInt(oxMatcher.group(1)));
        }
    }
}
