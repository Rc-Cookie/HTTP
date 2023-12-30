package de.rccookie.http.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Parse {

    /**
     * The parser types which may be used for trying to parse the contents of the request.
     */
    Class<? extends Parser>[] value();
}
