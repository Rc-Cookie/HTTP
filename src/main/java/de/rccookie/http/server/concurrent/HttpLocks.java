package de.rccookie.http.server.concurrent;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import de.rccookie.util.Arguments;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class to access the locks used by {@link ReadLocked} and {@link WriteLocked}.
 */
public final class HttpLocks {

    private HttpLocks() { }


    private static final Map<Class<?>, ReadWriteLock> LOCKS = new ConcurrentHashMap<>();


    /**
     * Returns the locks used by a {@link ReadLocked}s and {@link WriteLocked}s when
     * the given classes are specified, packaged as a single lock that locks the individual
     * locks in the same order as they will be locked in by the annotations.
     *
     * <p>If a single class key is specified, the result of this method is identical to
     * <code>HttpLocks.get(keys[0])</code>. If 0 class keys are specified, the returned
     * lock will have no effect when being locked or unlocked.</p>
     *
     * <p>This method is thread-safe Nevertheless, the result should probably be cached
     * locally.</p>
     *
     * @param keys The classes to get the locks for
     * @return The locks identified by those classes, in the same order that {@link ReadLocked}
     *         and {@link WriteLocked} will lock them, packaged as a single lock
     */
    public static ReadWriteLock getUnified(Class<?>... keys) {
        if(Arguments.checkNull(keys, "keys").length == 1)
            return get(keys[0]);
        ReadWriteLock[] locks = getAll(keys);
        Lock[] readLocks = new Lock[locks.length];
        Lock[] writeLocks = new Lock[locks.length];
        for(int i=0; i<locks.length; i++) {
            readLocks[i] = locks[i].readLock();
            writeLocks[i] = locks[i].writeLock();
        }
        Lock readLock = new MultiLock(readLocks);
        Lock writeLock = new MultiLock(writeLocks);

        return new ReadWriteLock() {
            @NotNull
            @Override
            public Lock readLock() {
                return readLock;
            }

            @NotNull
            @Override
            public Lock writeLock() {
                return writeLock;
            }
        };
    }

    /**
     * Returns the locks used by a {@link ReadLocked}s and {@link WriteLocked}s when
     * the given classes are specified, sorted in the same order as they will be locked
     * in by the annotations.
     *
     * <p>This method is thread-safe Nevertheless, the result should probably be cached
     * locally.</p>
     *
     * @param keys The classes to get the locks for
     * @return The locks identified by those classes, in the same order that {@link ReadLocked}
     *         and {@link WriteLocked} will lock them
     */
    public static ReadWriteLock[] getAll(Class<?>... keys) {
        keys = Arguments.checkNull(keys, "keys").clone();
        Arrays.sort(keys, Comparator.comparing(Class::getName));
        ReadWriteLock[] locks = new ReadWriteLock[keys.length];
        for(int i = 0; i < locks.length; i++)
            locks[i] = get(keys[i]);
        return locks;
    }

    /**
     * Returns the lock used by a {@link ReadLocked} or {@link WriteLocked} when the
     * given class is specified.
     *
     * <p>This method is thread-safe Nevertheless, the result should probably be cached
     * locally.</p>
     *
     * @param key The class to get the lock for
     * @return The lock identified by that class
     */
    public static ReadWriteLock get(Class<?> key) {
        return LOCKS.computeIfAbsent(key, $ -> new ReentrantReadWriteLock());
    }


    private static class MultiLock implements Lock {

        private final Lock[] locks;

        private MultiLock(Lock[] locks) {
            this.locks = locks;
        }

        @Override
        public void lock() {
            for(int i=0; i<locks.length; i++) {
                try {
                    locks[i].lock();
                } catch(Exception e) {
                    for(i--; i>=0; i--)
                        locks[i].unlock();
                    throw e;
                }
            }
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            for(int i=0; i<locks.length; i++) {
                try {
                    locks[i].lockInterruptibly();
                } catch(Exception e) {
                    for(i--; i>=0; i--)
                        locks[i].unlock();
                    throw e;
                }
            }
        }

        @Override
        public boolean tryLock() {
            for(int i=0; i<locks.length; i++) {
                try {
                    if(!locks[i].tryLock()) {
                        for(i--; i>=0; i--)
                            locks[i].unlock();
                        return false;
                    }
                } catch(Exception e) {
                    for(i--; i>=0; i--)
                        locks[i].unlock();
                    throw e;
                }
            }
            return true;
        }

        @Override
        public boolean tryLock(long time, @NotNull TimeUnit unit) throws InterruptedException {
            long nanos = unit.toNanos(time);
            long end;
            try {
                end = Math.addExact(System.nanoTime(), nanos);
            } catch(ArithmeticException e) {
                end = Long.MAX_VALUE;
            }

            for(int i=0; i<locks.length; i++) {
                try {
                    if(!locks[i].tryLock(end - System.nanoTime(), TimeUnit.NANOSECONDS)) {
                        for(i--; i>=0; i--)
                            locks[i].unlock();
                        return false;
                    }
                } catch(Exception e) {
                    for(i--; i>=0; i--)
                        locks[i].unlock();
                    throw e;
                }
            }
            return true;
        }

        @Override
        public void unlock() {
            RuntimeException ex = null;
            for(int i=0; i<locks.length; i++) {
                try {
                    locks[i].unlock();
                } catch(RuntimeException e) {
                    if(ex == null)
                        ex = e;
                    else ex.addSuppressed(e);
                }
            }
            if(ex != null)
                throw ex;
        }

        @NotNull
        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }
}
