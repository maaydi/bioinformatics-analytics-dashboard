package com.bioinformatics.dashboard.admin.validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ ElementType.PARAMETER, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FileSizeValidator.class)
public @interface ValidFileType {
    String message() default "Invalid file";

    String[] extensions() default {};

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
