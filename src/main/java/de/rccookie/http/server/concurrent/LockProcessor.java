package de.rccookie.http.server.concurrent;

import java.lang.reflect.Method;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import de.rccookie.http.HttpRequest;
import de.rccookie.http.server.HttpProcessor;
import de.rccookie.http.server.IllegalHttpProcessorException;
import de.rccookie.http.server.ThrowingRunnable;

class LockProcessor implements HttpProcessor {

    private final Lock lock;

    LockProcessor(boolean readLock, Class<?>[] lockClasses, Method method, boolean isOnMethod) {

        if(method.isAnnotationPresent(NotLocked.class) || (!isOnMethod && (method.isAnnotationPresent(ReadLocked.class) || method.isAnnotationPresent(WriteLocked.class)))) {
            lock = null;
            return;
        }

        if(lockClasses.length == 1 && lockClasses[0] == void.class) {
            LockTarget lockTarget = method.getDeclaringClass().getAnnotation(LockTarget.class);
            if(lockTarget == null)
                throw new IllegalHttpProcessorException((readLock ? "@ReadLocked" : "@WriteLocked")+" requires a lock target class with (value=) or with @LockTarget() to specify lock scope");
            lockClasses = lockTarget.value();
        }
        ReadWriteLock rwLock = HttpLocks.getUnified(lockClasses);
        this.lock = readLock ? rwLock.readLock() : rwLock.writeLock();
    }

    @SuppressWarnings("unused")
    LockProcessor(ReadLocked readLocked, Method method, boolean isOnMethod) {
        this(true, readLocked.value(), method, isOnMethod);
    }

    @SuppressWarnings("unused")
    LockProcessor(WriteLocked writeLocked, Method method, boolean isOnMethod) {
        this(false, writeLocked.value(), method, isOnMethod);
    }


    @Override
    public void process(HttpRequest.Received request, ThrowingRunnable runHandler) throws Exception {
        if(lock == null) {
            runHandler.run();
            return;
        }

        lock.lock();
        try {
            runHandler.run();
        } finally {
            lock.unlock();
        }
    }
}
