package com.github.rccookie.http;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import com.github.rccookie.util.Arguments;
import com.github.rccookie.util.Future;
import com.github.rccookie.util.ThreadedFutureImpl;

/**
 * Future implementation that only starts the computation if the result is requested
 * directly (using {@link #waitFor()}) or indirectly (using {@link #then} or {@link #onCancel}).
 *
 * @param <V> Content type
 */
class OnDemandFutureImpl<V> extends ThreadedFutureImpl<V> {

    static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private final Computation<V> computation;
    private boolean started = false;

    public OnDemandFutureImpl(Computation<V> computation) {
        this.computation = Arguments.checkNull(computation, "computation");
    }

    @Override
    public V waitFor() throws UnsupportedOperationException {
        startComputation();
        return super.waitFor();
    }

    @Override
    public Future<V> then(Consumer<? super V> action) {
        startComputation();
        return super.then(action);
    }

    @Override
    public Future<V> onCancel(Consumer<Exception> handler) {
        startComputation();
        return super.onCancel(handler);
    }

    synchronized void startComputation() {
        if(started) return;
        started = true;
        if(isCanceled()) return;
        EXECUTOR.submit(() -> {
            try {
                complete(computation.compute());
            } catch(Exception e) {
                fail(e);
            }
        });
    }
}
