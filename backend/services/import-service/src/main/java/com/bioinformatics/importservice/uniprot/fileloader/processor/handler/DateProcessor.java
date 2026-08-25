package com.bioinformatics.importservice.uniprot.fileloader.processor.handler;

import com.bioinformatics.importservice.uniprot.fileloader.processor.LineProcessor;
import com.bioinformatics.importservice.uniprot.fileloader.processor.ProteinParsingContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class DateProcessor implements LineProcessor {


    private static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("dd-MMM-yyyy")
            .toFormatter(Locale.ENGLISH);

    private static final Pattern VERSION_PATTERN = Pattern.compile("version (\\d+)");


    @Override
    public String getPrefix() {
        return "DT";
    }

    @Override
    public void process(String line, ProteinParsingContext context) {
        var entryBuilder = context.getEntryBuilder();
        var dateStr = line.split(",")[0].trim();
        var date = LocalDate.parse(dateStr, DATE_FORMATTER);
        if (line.contains("integrated")) {
            entryBuilder.integratedDate(date);
        } else if (line.contains("sequence version")) {
            entryBuilder.sequenceDate(date);
            var m = VERSION_PATTERN.matcher(line);
            if (m.find()) {
                entryBuilder.sequenceVersion(Short.parseShort(m.group(1)));
            }
        } else if (line.contains("entry version")) {
            entryBuilder.updatedDate(date);
            var m = VERSION_PATTERN.matcher(line);
            if (m.find()) {
                entryBuilder.entryVersion(Short.parseShort(m.group(1)));
            }
        }
    }
}
