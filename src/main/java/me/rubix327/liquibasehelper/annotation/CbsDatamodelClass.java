package me.rubix327.liquibasehelper.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ограничения @CbsDatamodelClass:<ol>
 *     <li>Можно устанавливать только на обычных классах (нельзя на внутренних, вложенных, локальных, анонимных классах, интерфейсах и перечислениях).</li>
 *     <li>Название класса должно совпадать с названием файла.</li>
 *     <li>Тег {@link #tag()} не должен повторяться в других классах.</li>
 *     <li>Параметр {@link #tag()} нельзя применять вместе с {@link #mapped()}=true. </li>
 * </ol>
 * @see #tag() Документация поля tag
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
@SuppressWarnings("unused")
public @interface CbsDatamodelClass {

    /**
     * Являются ли правила этого класса вложенными (в другие классы @CbsDatamodelClass).
     * <br><br>
     * Если true, то на основе этого класса не будут созданы самостоятельные правила.
     * Правила такого класса только встраиваются в классы-наследники.
     * Если у такого класса нет ни одного наследника, то его правила никогда не будут зарегистрированы.
     * <br><br>
     * Данный параметр предпочтительно использовать на мета-классах, для которых напрямую не создается TaskChange.<br>
     * <b>Примеры из cbscoreservices:</b> BaseEntityMeta, GuideMeta.
     * <br><br>
     * У такого класса не может быть указан {@link #tag()}.
     */
    boolean mapped() default false;

    /**
     * Название XML-тега, для которого будут действовать правила, указанные в данном классе.
     * <br><br>
     * Если тег явно не установлен, то он будет равен названию класса с маленькой буквы (класс AccIntentionMeta -> тег accIntentionMeta).
     * <br><br>
     * Не может быть использовать вместе с <b>mapped=true</b>.
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
