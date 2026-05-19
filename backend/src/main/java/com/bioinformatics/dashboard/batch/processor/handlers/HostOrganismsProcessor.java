package com.bioinformatics.dashboard.batch.processor.handlers;

import com.bioinformatics.dashboard.batch.processor.LineProcessor;
import com.bioinformatics.dashboard.batch.processor.ProteinParsingContext;
import com.bioinformatics.dashboard.gene.entity.HostOrganism;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class HostOrganismsProcessor implements LineProcessor {
    private static final Pattern OX_TAXID_PATTERN = Pattern.compile("NCBI_TaxID=(\\d+)");

    @Override
    public String getPrefix() {
        return "OH";
    }

    @Override
    public void process(String line, ProteinParsingContext context) {
        var hostBuilder = HostOrganism.builder();
        var oxMatch = OX_TAXID_PATTERN.matcher(line);
        if (oxMatch.find()) {
            hostBuilder.taxid(Integer.parseInt(oxMatch.group(1)));
        }
        var ohParts = line.split(";");
        if (ohParts.length >= 2) {
            hostBuilder.name(ohParts[1].trim());
            context.getHostOrganisms().add(hostBuilder.build());
        }
    }
}
