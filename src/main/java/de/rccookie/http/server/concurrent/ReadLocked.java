package de.rccookie.http.server.concurrent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.rccookie.http.server.annotation.HttpProcessorType;

/**
 * Methods annotated with {@link ReadLocked} and {@link WriteLocked} will acquire
 * a read-lock and write-lock, respectively, before executing the handler method,
 * and release the lock when the handler finishes (before the request is sent back.
 * Serialization of the response will be performed before releasing the lock though).
 *
 * <p>To define the scope of the lock, the {@link #value()} parameter must be specified.
 * Alternatively, the whole class can be annotated with {@link LockTarget}. At least one
 * of these two ways must be specified; if both are present, the value of {@link #value()}
 * will supersede the value specified with {@link LockTarget}. The lock will not actually
 * be performed using the class object as monitor, but serves as key to identify the lock.
 * In case the lock needs to be used elsewhere, the actual lock can be determined using
 * {@link HttpLocks#get(Class)} methods.</p>
 *
 * <p>Multiple lock targets can be specified in the locks. In that case, the locks will be
 * obtained in an <i>arbitrary order</i>, but <i>always in the same order</i>. The method
 * {@link HttpLocks#getAll(Class[])} returns the locks in that same order, and the
 * {@link HttpLocks#getUnified(Class[])} returns a single lock which locks those same locks
 * in the same order, packaged as a single lock.</p>
 *
 * <p>If {@link ReadLocked} or {@link WriteLocked} is present on the class, and also one
 * of them on a method in the class, that method will ignore the annotations on the class.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@HttpProcessorType(LockProcessor.class)
public @interface ReadLocked {

    /**
     * Specifies the scope of this lock, that is, all {@link ReadLocked}s and {@link WriteLocked}s
     * with the same class will operate on the same read-write-lock, and on a different lock than
     * when any other class was specified. If multiple classes are specified, all those locks will
     * be obtained, in an arbitrary but consistent order, that is, all locks have a total order
     * in which they will be obtained.
     *
     * <p>If this parameter is not specified, the class in which the annotation is used must be
     * annotated with {@link LockTarget} instead. If both are present, this value will supersede
     * any value from {@link LockTarget}.</p>
     */
    Class<?>[] value() default void.class;
}
