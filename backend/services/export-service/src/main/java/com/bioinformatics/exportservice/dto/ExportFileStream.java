package com.bioinformatics.exportservice.dto;

import com.bioinformatics.exportservice.writer.ExportFormatWriter;

import java.io.InputStream;

public record ExportFileStream(InputStream stream, String filename, long filesize, ExportFormatWriter formatWriter) {
}
