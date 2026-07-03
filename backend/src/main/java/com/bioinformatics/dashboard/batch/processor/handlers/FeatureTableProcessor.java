package com.bioinformatics.dashboard.batch.processor.handlers;

import com.bioinformatics.dashboard.batch.processor.LineProcessor;
import com.bioinformatics.dashboard.batch.processor.ProteinParsingContext;
import com.bioinformatics.dashboard.providers.postgres.gene.entity.ProteinFeature;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class FeatureTableProcessor implements LineProcessor {
    private static final Pattern FEATURE_PATTERN = Pattern.compile("([A-Z_]+)\\s+(\\d+)\\.\\.(\\d+)");

    @Override
    public String getPrefix() {
        return "FT";
    }

    @Override
    public void process(String line, ProteinParsingContext context) {
        var featureBuilders = context.getFeatureBuilders();
        if (!line.contains("/")) {
            var mt = FEATURE_PATTERN.matcher(line);
            if (mt.find()) {
                var ft = ProteinFeature.builder()
                        .featureType(mt.group(1))
                        .startPos(Integer.parseInt(mt.group(2)))
                        .endPos(Integer.parseInt(mt.group(3)));
                featureBuilders.add(ft);
            }
        } else {
            if (!featureBuilders.isEmpty()) {
                var last = featureBuilders.getLast().build();
                if (line.startsWith("/id") && last.getFeatureId() == null) {
                    featureBuilders.getLast().featureId(line.split("=")[1]);
                } else if (line.startsWith("/note") && last.getNote() == null) {
                    featureBuilders.getLast().note(line.split("=")[1]);
                } else if (line.startsWith("/evidence") && last.getEvidence() == null) {
                    featureBuilders.getLast().evidence(line.split("=")[1]);
                }
            }
        }
    }
}
