package me.rubix327.liquibasehelper.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@SuppressWarnings("unused")
public @interface CbsDatamodelField {

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

    /**
     * Является ли XML-тег обязательным внутри мета-сущности, в которой расположен.<br>
     * Если true, то на родительском теге ({@link CbsDatamodelClass}),
     * будет выведено предупреждение с просьбой добавить этот тег внутри родительского.
     */
    boolean required() default false;

    /**
     * Максимальная длина значения xml-тега.<br>
     * Если указано 0 или меньше, то тег не проверяется по этому признаку.
     */
    int maxLength() default 0;

    /**
     * Возможные значения для этого xml-тега.<br>
     * Заполняются в виде строки или массива строк.
     * Через двоеточие от значения можно указать комментарий к значению.
     * <br><br>
     * Если массив пустой, то тег по этому признаку не проверяется.
     * <br><br>
     * Если это поле в аннотации объявлено вместе с {@link #availableValuesEnum()},
     * то тег проверяется только по значениям availableValuesEnum.
     * <br><br>
     * Пример в виде одной строки: {"0:Нет;1:Да"}<br>
     * Пример в виде массива строк: {"0:Нет", "1:Да"}.
     * @see #availableValuesEnum() Заполнение через ссылку на перечисление
     * @see #availableValuesEnumPath() Заполнение через полный путь до перечисления
     */
    String[] availableValues() default "";

    /**
     * Возможные значения для этого xml-тега.<br>
     * Заполняется в виде ссылки на enum.
     * <br><br>
     * <b>Имеет приоритет над {@link #availableValues()}.</b>
     * <br><br>
     * Внутри enum неподходящие значения можно отмечать {@link CbsDatamodelIgnore}.<br>
     * Значение из enumeration получаются в следующем приоритете:
     * <ol>
     *     <li>Из аннотации {@link CbsDatamodelValue} (поле value)</li>
     *     <li>Из java-поля value, объявленного внутри enumeration вместе с подходящим конструктором</li>
     *     <li>Берется название самого поля, как есть</li>
     * </ol>
     * @see #availableValues() Заполнение в виде строки или массива строк
     * @see #availableValuesEnumPath() Заполнение через полный путь до перечисления
     */
    Class<?> availableValuesEnum() default Object.class;

    /**
     * Возможные значения для этого xml-тега.<br>
     * Заполняется в виде полного пути до класса-перечисления.
     * <br><br>
     * Функционально - то же самое, что {@link #availableValuesEnum()}, но только в виде строки
     * (на случай, если enum недоступен из модуля metaloader).
     * <br><br>
     * <b>Имеет приоритет над {@link #availableValues()} и {@link #availableValuesEnum()}.</b>
     * <br><br>
     * Внутри enum неподходящие значения можно отмечать {@link CbsDatamodelIgnore}.<br>
     * Значение из enumeration получаются в следующем приоритете:
     * <ol>
     *     <li>Из аннотации {@link CbsDatamodelValue} (поле value)</li>
     *     <li>Из java-поля value, объявленного внутри enumeration вместе с подходящим конструктором</li>
     *     <li>Берется название самого поля, как есть</li>
     * </ol>
     * @see #availableValues() Заполнение в виде строки или массива строк
     * @see #availableValuesEnum() Заполнение через ссылку на перечисление
     */
    String availableValuesEnumPath() default "";

    /**
     * Тип значения тега.<br><br>
     * Может быть пяти видов: String, Long, Double, Boolean, Date.<br>
     * <ul>
     *     <li>Если указано String, то значение тега никак не проверяется.</li>
     *     <li>Если указано Long, значение проверяется путем парсинга целого числа.</li>
     *     <li>Если указано Double, значение проверяется путем парсинга числа с плавающей точкой.</li>
     *     <li>Если указано Boolean, значение может быть только 0 или 1.</li>
     *     <li>Если указано Date, значение должно быть в формате даты.</li>
     * </ul>
     */
    Class<?> type() default String.class;

}

