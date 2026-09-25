package com.bioinformatics.common.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public final class NonClosingOutputStream extends FilterOutputStream {
    public NonClosingOutputStream(OutputStream outputStream) {
        super(outputStream);
    }

    @Override
    public void close() throws IOException {
        flush();
    }
}
