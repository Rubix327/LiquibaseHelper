package me.rubix327.liquibasehelper;

import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.psi.*;
import me.rubix327.liquibasehelper.inspection.model.DatamodelClassCheckResponse;
import me.rubix327.liquibasehelper.settings.CbsAnnotation;
import me.rubix327.liquibasehelper.settings.StaticSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AnnotationUtils {

    public static boolean getBooleanValueOrDefault(PsiAnnotationMemberValue param, boolean defaultValue){
        Boolean value = getBooleanValue(param);
        return value == null ? defaultValue : value;
    }

    @Nullable
    public static Boolean getBooleanValue(PsiAnnotationMemberValue param){
        if (param instanceof PsiLiteralExpression paramLiteral){
            if (paramLiteral.getValue() instanceof Boolean booleanValue){
                return booleanValue;
            }
        }
        return null;
    }

    @Nullable
    public static String getStringValue(PsiAnnotationMemberValue param){
        if (param instanceof PsiLiteralExpression paramLiteral){
            if (paramLiteral.getValue() instanceof String stringValue && Utils.isNotBlank(stringValue)){
                return stringValue;
            }
        }
        return null;
    }

    /**
     * Извлечь значение ключа из выражения.
     */
    @Nullable
    public static String resolveStringValue(@NotNull PsiExpression expression) {
        if (expression instanceof PsiLiteralExpression literalExpression) {
            // Если это строковый литерал
            Object value = literalExpression.getValue();
            return value instanceof String ? (String) value : null;
        } else if (expression instanceof PsiReferenceExpression referenceExpression) {
            // Если это ссылка на переменную
            var resolvedElement = referenceExpression.resolve();
            if (resolvedElement instanceof PsiVariable variable) {
                var initializer = variable.getInitializer();
                if (initializer != null) {
                    return resolveStringValue(initializer); // Рекурсивно извлекаем значение
                }
            }
        }
        return null; // Не удалось определить значение
    }

    public static int getIntegerValueOrDefault(PsiAnnotationMemberValue param, int defaultValue){
        Integer value = getIntegerValue(param);
        return value == null ? defaultValue : value;
    }

    @Nullable
    public static Integer getIntegerValue(PsiAnnotationMemberValue param){
        if (param instanceof PsiLiteralExpression paramLiteral){
            if (paramLiteral.getValue() instanceof Integer integerValue){
                return integerValue;
            }
        }
        return null;
    }

    /**
     * Найти аннотацию над объектом.
     */
    @Nullable
    public static PsiAnnotation findAnnotation(@NotNull PsiJvmModifiersOwner annotationHolder, @NotNull CbsAnnotation cbsAnnotation){
        if (StaticSettings.SEARCH_ANNOTATION_BY_QUALIFIED_NAME){
            return annotationHolder.getAnnotation(cbsAnnotation.getQualifiedName());
        }

        for (PsiAnnotation annotation : annotationHolder.getAnnotations()) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName == null) continue;

            if (qualifiedName.contains(cbsAnnotation.getShortName())){
                return annotation;
            }
        }
        return null;
    }

    public static boolean isNotDatamodelClass(@Nullable PsiClass psiClass){
        return !checkIsDatamodelClass(psiClass).isDatamodelClass();
    }

    /**
     * Проверить, что класс не является CbsDatamodelClass.<br>
     * Если этот метод возвращает true, то это гарантирует, что как минимум одно из следующих условий верно:<ul>
     * <li>Указанный psiClass == null</li>
     * <li>У указанного psiClass нет qualifiedName</li>
     * <li>Указанный psiClass не находится в пакете metaloader</li>
     * <li>Указанный psiClass вложен в другие классы</li>
     * <li>Указанный psiClass является перечислением (enum)</li>
     * <li>Над указанным psiClass нет аннотации @CbsDatamodelClass</li>
     * </ul>
     */
    public static DatamodelClassCheckResponse checkIsDatamodelClass(@Nullable PsiClass psiClass){
        if (psiClass == null) return new DatamodelClassCheckResponse(psiClass, false, "PsiClass is null");
        if (Object.class.getName().equals(psiClass.getQualifiedName())) return new DatamodelClassCheckResponse(psiClass, false, "PsiClass is Object");
        if (psiClass.getQualifiedName() == null || !psiClass.getQualifiedName().contains(".metaloader.")) return new DatamodelClassCheckResponse(psiClass, false, "Qualified name is null or does not contain .metaloader. inside");
        if (psiClass.getContainingClass() != null) return new DatamodelClassCheckResponse(psiClass, false, "The class is inner");
        if (psiClass.isEnum()) return new DatamodelClassCheckResponse(psiClass, false, "The class is enum");
        if (findAnnotation(psiClass, CbsAnnotation.CbsDatamodelClass.INSTANCE) == null) return new DatamodelClassCheckResponse(psiClass, false, "The class does not have @CbsDatamodelClass annotation");
        return new DatamodelClassCheckResponse(psiClass, true);
    }

    public static boolean isDatamodelMappedClass(@Nullable PsiClass psiClass){
        if (isNotDatamodelClass(psiClass)) return false;
        assert psiClass != null;

        PsiAnnotation annotation = findAnnotation(psiClass, CbsAnnotation.CbsDatamodelClass.INSTANCE);
        if (annotation == null) return false;

        PsiAnnotationMemberValue isMappedMember = annotation.findDeclaredAttributeValue(CbsAnnotation.CbsDatamodelClass.Fields.MAPPED);
        if (isMappedMember instanceof PsiLiteralExpression isMappedBoolean){
            if (isMappedBoolean.getValue() instanceof Boolean isMappedValue){
                return isMappedValue;
            }
        }
        return false;
    }

    @Nullable
    public static String getCbsDatamodelClassAnnotationFieldStringValue(@NotNull PsiClass psiClass, String field){
        if (isNotDatamodelClass(psiClass)) return null;
        return getAnnotationFieldStringValue(psiClass, CbsAnnotation.CbsDatamodelClass.INSTANCE, field);
    }

    @Nullable
    public static String getAnnotationFieldStringValue(@NotNull PsiClass psiClass, CbsAnnotation annotation, String fieldName){
        PsiAnnotation psiAnnotation = findAnnotation(psiClass, annotation);
        if (psiAnnotation == null){
            return null;
        }

        return getStringValue(psiAnnotation.findDeclaredAttributeValue(fieldName));
    }

    public static PsiNameValuePair getPsiNameValuePairFromAnnotation(@NotNull PsiAnnotation annotation, String attributeName){
        JvmAnnotationAttribute jvmAnnotationAttribute = annotation.findAttribute(attributeName);
        if (jvmAnnotationAttribute == null) return null;
        return jvmAnnotationAttribute instanceof PsiNameValuePair attr ? attr : null;
    }

}
