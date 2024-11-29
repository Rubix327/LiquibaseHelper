package me.rubix327.liquibasehelper.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
@SuppressWarnings("unused")
public @interface CbsDatamodelClass {

    boolean mapped() default false;
    String tag() default "";
    String comment() default "";
    String description() default "";

}
