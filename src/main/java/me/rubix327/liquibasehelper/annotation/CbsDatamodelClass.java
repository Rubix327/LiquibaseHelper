package me.rubix327.liquibasehelper.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Нельзя устанавливать на внутренних классах.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
@SuppressWarnings("unused")
public @interface CbsDatamodelClass {

    /**
     * Если true, то данный класс подразумевается родительским, и для него не нужно создавать отдельных правил.
     * У такого класса не может быть указан {@link #tag()}. Правила в таком классе только встраиваются в классы-наследники.
     */
    boolean mapped() default false;

    /**
     * Название XML-тега, для которого будут действовать правила, указанные в данном классе.
     */
    String tag() default "";

    /**
     * Комментарий к тегу.<br>
     * Если пустой, то не показывается при наведении на тег.
     */
    String comment() default "";

    /**
     * Подробное описание к тегу.<br>
     * Отображается под комментарием ({@link #comment()}).<br>
     * Если пустое, то не показывается при наведении на тег.
     */
    String description() default "";

}
