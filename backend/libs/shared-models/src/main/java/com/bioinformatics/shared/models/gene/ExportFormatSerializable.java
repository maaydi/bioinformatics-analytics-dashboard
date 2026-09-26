package com.bioinformatics.shared.models.gene;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface ExportFormatSerializable extends Serializable {
    Map<String, Object> row();

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
            return null;
        }

        return String.join("; ", values);
    }

    default String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        return String.join("; ", values);
    }
}
