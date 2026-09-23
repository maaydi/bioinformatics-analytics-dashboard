package com.bioinformatics.common.gene.dto;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Contract for objects that can be serialized to CSV.
 * <p>
 * Implementations provide a header and a row representation. Values are
 * escaped and quoted by  to be safe for CSV output.
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

}
