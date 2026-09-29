package com.bioinformatics.exportservice.dto;

public interface ExportFormat {

    /**
     * Return the file extension (without a leading dot) used for files produced by
     * this writer (e.g. "csv", "json", "xlsx").
     *
     * @return file extension string
     */
    String getFileExtension();

    /**
     * Return the HTTP content type (MIME type) that should be used when serving
     * the generated file (e.g. "text/csv;charset=UTF-8", "application/json",
     * "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet").
     *
     * @return content type string
     */
    String getContentType();

    /**
     * Used for enum name()
     *
     */
    String name();

}
