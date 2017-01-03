package com.sun.tools.hat.internal.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * A ViewGetter indicates that the method is used by the mustache views.
 */
@Documented
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface ViewGetter {
}
