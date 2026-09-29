package com.bioinformatics.common.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

/**
 * Thin delegating wrapper for an {@link ItemStreamReader}.
 *
 * <p>This keeps batch reader composition simple by forwarding lifecycle calls to a delegate while
 * preserving the original reader contract and restart semantics. It is useful when a reader needs to
 * add cross-cutting behavior such as logging, metrics, or buffering without modifying the delegate.
 *
 * @param <T> item type returned by the wrapped reader
 */
@RequiredArgsConstructor
@Slf4j
public class DelegatingItemStreamReader<T> implements ItemStreamReader<T> {

    private final ItemStreamReader<T> delegate;

    @Override
    public T read() throws Exception {
        return delegate.read();
    }

    @Override
    public void open(@NonNull ExecutionContext executionContext) throws ItemStreamException {
        log.debug("Opening delegated batch reader {} with execution context size={}",
                delegate.getClass().getSimpleName(), executionContext.size());
        delegate.open(executionContext);
    }

    @Override
    public void update(@NonNull ExecutionContext executionContext) throws ItemStreamException {
        log.debug("Updating delegated batch reader {} with execution context size={}",
                delegate.getClass().getSimpleName(), executionContext.size());
        delegate.update(executionContext);
    }

    @Override
    public void close() throws ItemStreamException {
        log.debug("Closing delegated batch reader {}", delegate.getClass().getSimpleName());
        delegate.close();
    }
}
