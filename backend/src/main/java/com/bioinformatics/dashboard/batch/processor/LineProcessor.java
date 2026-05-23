package com.bioinformatics.dashboard.batch.processor;

public interface LineProcessor {

    String getPrefix();

    void process(String line, ProteinParsingContext context);

    /**
     * Helper to extract values from String <KEY=VALUE;>
     * "Full=Putative transcription factor;" -> "Putative transcription factor"
     */
    default String extractValue(String line, String key) {
        var start = line.indexOf(key) + key.length();
        var end = line.indexOf(";", start);
        if (end == -1)
            end = line.length();
        return line.substring(start, end).trim();
    }

    /**
     * Remove extra space and end "." in a value
     *
     */
    default String cleanStr(final String value) {
        var v = value.trim();
        if (v.endsWith(".")) {
            return v.substring(0, v.length() - 1);
        }
        return v;
    }
}
