package com.bioinformatics.importservice.uniprot.fileloader.reader;


import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

@RequiredArgsConstructor
public class DelegatingItemStreamReader<T> implements ItemStreamReader<T> {

    private final ItemStreamReader<T> delegate;

    @Override
    public T read() throws Exception {
        return delegate.read();
    }

    @Override
    public void open(@NonNull ExecutionContext executionContext) throws ItemStreamException {
        delegate.open(executionContext);
    }

    @Override
    public void update(@NonNull ExecutionContext executionContext) throws ItemStreamException {
        delegate.update(executionContext);
    }

    @Override
    public void close() throws ItemStreamException {
        delegate.close();
    }
}
