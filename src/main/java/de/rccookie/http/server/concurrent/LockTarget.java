package de.rccookie.http.server.concurrent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used to collectively set the {@link ReadLocked#value()}
 * and {@link WriteLocked#value()} fields for all lock annotations present in the
 * annotated class. If the lock annotations also specifies a lock target, that
 * target will be used instead of the scope specified by this annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface LockTarget {

    /**
     * This annotation can be used to collectively set the {@link ReadLocked#value()}
     * and {@link WriteLocked#value()} fields for all lock annotations present in the
     * annotated class. If the lock annotations also specifies a lock target, that
     * target will be used instead of the scope specified by this annotation.
     */
    Class<?>[] value();
}
