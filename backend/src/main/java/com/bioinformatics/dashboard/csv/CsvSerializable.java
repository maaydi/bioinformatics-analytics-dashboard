package com.bioinformatics.dashboard.csv;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Contract for objects that can be serialized to CSV.
 * <p>
 * Implementations provide a header and a row representation. Values are
 * escaped and quoted by {@link #format(Object)} to be safe for CSV output.
 */
public interface CsvSerializable extends Serializable {


    default String header() {
        return Arrays.stream(this.getClass().getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.joining(separator()));
    }

    String row();

    default String separator() {
        return ",";
    }

    default String format(Object value) {
        if (value == null) {
            return "\"\"";
        }

        var escaped = value.toString()
                .replace("\"", "\"\"")
                .replace("\n", " ")
                .replace("\r", " ");

        return "\"" + escaped + "\"";
    }

    default String joinArray(String[] values) {
        if (values == null || values.length == 0) {
            return "";
        }

        return String.join("|", values);
    }

    default String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        return String.join("|", values);
    }
}
