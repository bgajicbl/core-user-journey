package com.myedspace.cuj.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method parameter to be resolved to the {@link com.myedspace.cuj.domain.Student}
 * that {@link AuthInterceptor} authenticated for this request. Only usable on paths the
 * interceptor is registered against (see WebConfig) — using it elsewhere fails fast rather than
 * silently returning null.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentStudent {
}
