package org.satel.eip.project14.adapter.pyramid.domain.generic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TargetEntity {
    @SuppressWarnings("rawtypes")
    Class value();
}
