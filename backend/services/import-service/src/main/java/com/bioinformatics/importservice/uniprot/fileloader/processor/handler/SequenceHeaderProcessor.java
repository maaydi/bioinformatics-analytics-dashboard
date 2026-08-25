package com.bioinformatics.importservice.uniprot.fileloader.processor.handler;

import com.bioinformatics.importservice.uniprot.fileloader.processor.LineProcessor;
import com.bioinformatics.importservice.uniprot.fileloader.processor.ProteinParsingContext;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class SequenceHeaderProcessor implements LineProcessor {
    private static final Pattern SQ_MW_PATTERN = Pattern.compile("(\\d+)\\s+MW");
    private static final Pattern SQ_CRC64_PATTERN = Pattern.compile("([A-F0-9]+)\\s+CRC64");

    @Override
    public String getPrefix() {
        return "SQ";
    }

    @Override
    public void process(String line, ProteinParsingContext context) {
        var mwMatcher = SQ_MW_PATTERN.matcher(line);
        if (mwMatcher.find()) {
            context.getEntryBuilder().molecularWeight(Integer.parseInt(mwMatcher.group(1)));
        }
        var crcMatcher = SQ_CRC64_PATTERN.matcher(line);
        if (crcMatcher.find()) {
            context.getEntryBuilder().sequenceChecksum(crcMatcher.group(1));
        }
        context.setInSequence(true);
    }
}
