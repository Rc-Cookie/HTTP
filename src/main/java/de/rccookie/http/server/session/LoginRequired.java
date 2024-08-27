package de.rccookie.http.server.session;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.rccookie.http.server.annotation.HttpProcessorType;

@HttpProcessorType(SessionProcessor.class)
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginRequired {

    Class<? extends LoginSessionManager<?>> manager() default NoTypeSpecified.class;

    Class<?> session() default NoTypeSpecified.class;

    int adminLevel() default 0;

    boolean redirect() default false;
}
