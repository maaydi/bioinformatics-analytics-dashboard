package com.bioinformatics.exportservice.writer;

import com.bioinformatics.exportservice.dto.ExportFormat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * Strategy interface for writing export data in a specific file format.
 *
 * <p>Implementations must be able to write a header and rows to the provided
 * {@link OutputStream}. Implementations should not close streams they did not
 * open unless explicitly required by the caller (the {@link #close(OutputStream)}
 * method is provided for implementations that need to finalize/flush format-specific
 * containers such as Excel workbooks).</p>
 *
 * <p>All methods are allowed to throw {@link IOException} on I/O failures.
 * Implementations should be careful about character encoding (UTF-8 is expected
 * across the application) and about producing streaming-friendly output since
 * writers will be used inside chunked batch steps.</p>
 */
public interface ExportFormatWriter {

    /**
     * Write the header row (column names) for the export file.
     *
     * @param fields ordered list of field names to write as columns
     * @param out    the output stream to write to (must not be null)
     * @throws IOException when writing fails
     */
    void writeHeader(List<String> fields, OutputStream out) throws IOException;

    /**
     * Write a single data row. The implementation must write the values in the
     * same order as {@code fields}.
     *
     * @param row    map of field name -> value; implementations should handle missing
     *               keys and null values in a null-safe manner
     * @param fields ordered list of field names determining output column order
     * @param out    the output stream to write to (must not be null)
     * @throws IOException when writing fails
     */
    void writeRow(Map<String, Object> row, List<String> fields, OutputStream out) throws IOException;

    /**
     * Finalize writer state for the provided output stream. For stream formats
     * (CSV/TSV/JSON) this may be a no-op; for container formats like Excel this
     * should flush and close any internal resources tied to the stream.
     *
     * <p>Callers remain responsible for closing the underlying {@link OutputStream}
     * unless the implementation documents otherwise.</p>
     *
     * @param out the output stream previously used for writing
     * @throws IOException when finalization fails
     */
    void close(OutputStream out) throws IOException;


    /**
     * Return the {@link ExportFormat} enum value that identifies the format
     * produced by this writer implementation.
     *
     * <p>This is used by the export pipeline to select a writer and to determine
     * filename extension and content-type behaviour at runtime.</p>
     *
     * @return the corresponding ExportFormat
     */
    ExportFormat getFormat();

}
