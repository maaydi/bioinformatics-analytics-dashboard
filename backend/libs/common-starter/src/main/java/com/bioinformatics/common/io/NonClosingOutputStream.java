package com.bioinformatics.common.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Output stream wrapper that flushes but intentionally does not close the underlying stream.
 *
 * <p>This is useful for streaming pipelines where the parent component owns the lifecycle of the
 * underlying resource and must keep it open for downstream processing after the writer finishes.
 */
public final class NonClosingOutputStream extends FilterOutputStream {
    public NonClosingOutputStream(OutputStream outputStream) {
        super(outputStream);
    }

    @Override
    public void close() throws IOException {
        flush();
    }
}
