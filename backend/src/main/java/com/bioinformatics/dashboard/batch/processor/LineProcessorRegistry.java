package com.bioinformatics.dashboard.batch.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Manages operations and logic for LineProcessorRegistry.
 */
@Component
@Slf4j
public class LineProcessorRegistry {
    private final Map<String, LineProcessor> lineProcessors;

    public LineProcessorRegistry(List<LineProcessor> lineProcessors) {
        this.lineProcessors = lineProcessors
                .stream()
                .collect(Collectors.toMap(LineProcessor::getPrefix, Function.identity()));
        log.info("Line Processor Regsitry : found {} processors", lineProcessors.size());

    }

    public void process(String line, ProteinParsingContext context) {
        if (line.length() < 5)
            return;
        var prefix = line.substring(0, 2);
        var data = line.substring(5).trim();
        if (context.isInSequence()) {
            if (!line.startsWith("//")) {
                context.getSequenceBuilder().append(line.replaceAll("\\s+", ""));
            }
            return;
        }
        var processor = lineProcessors.get(prefix);
        if (processor != null) {
            processor.process(data, context);
        }

    }

}
