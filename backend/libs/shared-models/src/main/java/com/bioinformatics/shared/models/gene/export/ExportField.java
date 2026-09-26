package com.bioinformatics.shared.models.gene.export;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.RECORD_COMPONENT)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExportField {
    String name() default "";

    String displayName() default "";

    Class<?> dataType() default String.class;

    String description() default "";

    boolean available() default true;
}
