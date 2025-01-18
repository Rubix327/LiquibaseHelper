package me.rubix327.liquibasehelper.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.navigation.NavigationItem;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.PsiUtil;
import me.rubix327.liquibasehelper.AnnotationUtils;
import me.rubix327.liquibasehelper.Utils;
import me.rubix327.liquibasehelper.inspection.model.TagRulesContainer;
import me.rubix327.liquibasehelper.inspection.quickfix.*;
import me.rubix327.liquibasehelper.locale.Localization;
import me.rubix327.liquibasehelper.log.MainLogger;
import me.rubix327.liquibasehelper.settings.CbsAnnotation;
import me.rubix327.liquibasehelper.settings.StaticSettings;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static me.rubix327.liquibasehelper.AnnotationUtils.getPsiNameValuePairFromAnnotation;
import static me.rubix327.liquibasehelper.inspection.quickfix.RemovePsiElementQuickFix.getRemovePsiElementFix;
import static me.rubix327.liquibasehelper.settings.CbsAnnotation.CbsDatamodelClass;
import static me.rubix327.liquibasehelper.settings.CbsAnnotation.CbsDatamodelField;
import static me.rubix327.liquibasehelper.settings.CbsAnnotation.CbsDatamodelField.Fields.*;

public class AnnotationInspector extends LocalInspectionTool {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @Override
            public void visitClass(@NotNull PsiClass psiClass) {
                if (!StaticSettings.ENABLE_INSPECTIONS){
                    return;
                }

                RulesManager instance = RulesManager.getInstance(psiClass.getProject());

                checkClass(instance, psiClass);
                updateClassesUsingEnums(instance, psiClass);
            }

            public void checkClass(@NotNull RulesManager instance, @NotNull PsiClass psiClass){
                PsiAnnotation annotation = AnnotationUtils.findAnnotation(psiClass, CbsDatamodelClass.INSTANCE);
                if (annotation == null) return;

                // Проверка на то, что класс является обычным
                if (psiClass.getContainingClass() != null || psiClass.isEnum() || psiClass.isInterface()) {
                    String message;
                    // Внутренние, вложенные классы (здесь не проверяются локальные и анонимные)
                    if (psiClass.getContainingClass() != null){
                        message = Localization.message("class.warn.inner-classes", CbsDatamodelClass.SHORT_NAME);
                    // Перечисления
                    } else if (psiClass.isEnum()){
                        message = Localization.message("class.warn.enumerations", CbsDatamodelClass.SHORT_NAME);
                    // Интерфейсы
                    } else {
                        message = Localization.message("class.warn.interfaces", CbsDatamodelClass.SHORT_NAME);
                    }

                    instance.removeRulesOfClass(psiClass, "Class is either inner, nested, enum or interface", "AnnotationInspector: checkClass");
                    Utils.registerError(holder, annotation,
                            new RemovePsiElementQuickFix(annotation, Localization.message("field.quickfix.delete-annotation", CbsDatamodelClass.SHORT_NAME)),
                            message);
                    return;
                }

                // Проверка на то, чтобы название класса совпадало с названием файла
                if (Utils.isClassAndFileNamesNotMatch(psiClass)){
                    instance.removeRulesOfClass(psiClass, "Class name does not match file name", "AnnotationInspector: checkClass");

                    LocalQuickFix fix = null;
                    if (psiClass.getContainingFile() != null) {
                        String newClassName = psiClass.getContainingFile().getName()
                                .replace(".java", "")
                                .replace(".class", "");
                        fix = new RenameClassQuickFix(psiClass, newClassName);
                    }

                    Utils.registerError(holder, annotation, fix, Localization.message("class.warn.class-name-not-match"));
                    return;
                }

                // Тег не может быть объявлен вместе с mapped=true
                if (AnnotationUtils.isDatamodelMappedClass(psiClass)){
                    PsiNameValuePair mapped = getPsiNameValuePairFromAnnotation(annotation, CbsDatamodelClass.Fields.MAPPED);
                    PsiNameValuePair tag = getPsiNameValuePairFromAnnotation(annotation, CbsDatamodelClass.Fields.TAG);
                    if (tag != null && mapped != null){
                        if (AnnotationUtils.getBooleanValueOrDefault(mapped.getValue(), false)){
                            Utils.registerError(holder, tag, getRemovePsiElementFix(tag), Localization.message("class.warn.mapped-and-tag"));
                            return;
                        }
                    }
                }

                String thisClassQualifiedName = psiClass.getQualifiedName();
                String currentDatamodelName = RulesManager.getDatamodelTagOfClass(psiClass);

                if (thisClassQualifiedName == null) return;
                if (currentDatamodelName == null) return;

                // Удаляем мусорные правила в процессе изменения tag у аннотации @CbsDatamodelClass
                String savedDatamodelName = instance.getDatamodelValueFromRegistry(thisClassQualifiedName);
                if (!currentDatamodelName.equals(savedDatamodelName)){
                    instance.removeRulesByTagNameAndClass(thisClassQualifiedName, savedDatamodelName);
                    instance.putDatamodelValueToRegistry(thisClassQualifiedName, currentDatamodelName);
                }

                // Собираем правила для класса и его родителей
                instance.handleClassAndSuperClasses(psiClass, "AnnotationInspector: checkClass: exact class");

                // Обновляем правила наследников, в случае если были изменены правила в родителе
                for (PsiClass inheritor : getInheritors(psiClass, ".metaloader.")) {
                    instance.handleClassAndSuperClasses(inheritor, "AnnotationInspector: checkClass: inheritor");
                }

                // Проверка на совпадающие теги у классов
                List<TagRulesContainer> rulesFromDatamodelName = instance.getRulesContainerListByTagName(currentDatamodelName);
                if (rulesFromDatamodelName.size() > 1){
                    rulesFromDatamodelName.removeIf(e -> thisClassQualifiedName.equals(e.getMetaClassPath()));
                    String anotherClass = rulesFromDatamodelName.get(0).getMetaClassPath();

                    PsiClass anotherPsiClass = Utils.findPsiClassByQualifiedName(psiClass.getProject(), anotherClass);
                    if (anotherPsiClass != null){
                        LocalQuickFix fix = new OpenPsiElementQuickFix(anotherPsiClass, Localization.message("class.quickfix.open-another-class", anotherPsiClass.getName()));
                        Utils.registerError(holder, annotation, fix, Localization.message("class.warn.already-defined", currentDatamodelName, anotherPsiClass.getName()));
                    }
                }

                super.visitClass(psiClass);
            }

            void updateClassesUsingEnums(@NotNull RulesManager instance, @NotNull PsiClass psiClass){
                // Если обновляется enum, который используется в мета-классах, то нужно обновить правила в этих мета-классах
                Set<PsiClass> classesToUpdate = instance.getClassesUsingThisEnum(psiClass);
                if (classesToUpdate != null){
                    MainLogger.info(instance.getProject(), "Updating classes using enum %s: %s", psiClass.getName(), classesToUpdate.stream().map(NavigationItem::getName).toList());
                    for (PsiClass classToUpdate : classesToUpdate) {
                        if (classToUpdate == null || !classToUpdate.isValid()) continue;
                        instance.handleClassAndSuperClasses(classToUpdate, "AnnotationInspector: updateClassesUsingEnums: exact class");
                        for (PsiClass inheritor : getInheritors(classToUpdate, ".metaloader.")) {
                            instance.handleClassAndSuperClasses(inheritor, "AnnotationInspector: updateClassesUsingEnums: inheritor");
                        }
                    }
                }
            }

            @Override
            public void visitField(@NotNull PsiField field) {
                if (!StaticSettings.ENABLE_INSPECTIONS){
                    return;
                }

                super.visitField(field);
                checkForFieldConstraints(field, holder);
            }
        };
    }

    /**
     * Найти наследников определенного класса
     */
    public static Collection<PsiClass> getInheritors(PsiClass psiClass, String mustContainPath) {
        GlobalSearchScope scope = GlobalSearchScope.allScope(psiClass.getProject());
        return ClassInheritorsSearch.search(psiClass, scope, true)
                .findAll().stream()
                .filter(c -> c.getQualifiedName() != null)
                .filter(c -> Utils.isBlank(mustContainPath) || c.getQualifiedName().contains(mustContainPath))
                .toList();
    }

    public void checkForFieldConstraints(PsiField field, ProblemsHolder holder) {
        PsiAnnotation annotation = AnnotationUtils.findAnnotation(field, CbsDatamodelField.INSTANCE);
        if (annotation == null){
            return;
        }

        PsiNameValuePair availableValuesPair = getPsiNameValuePairFromAnnotation(annotation, AVAILABLE_VALUES);
        PsiNameValuePair availableValuesEnumPair = getPsiNameValuePairFromAnnotation(annotation, AVAILABLE_VALUES_ENUM);
        PsiNameValuePair availableValuesEnumPathPair = getPsiNameValuePairFromAnnotation(annotation, AVAILABLE_VALUES_ENUM_PATH);
        PsiNameValuePair typePair = getPsiNameValuePairFromAnnotation(annotation, TYPE);
        PsiNameValuePair maxLengthPair = getPsiNameValuePairFromAnnotation(annotation, MAX_LENGTH);

        // На классе нет аннотации @CbsDatamodelClass
        PsiClass psiClass = field.getContainingClass();
        if (psiClass != null){
            if (AnnotationUtils.findAnnotation(psiClass, CbsDatamodelClass.INSTANCE) == null){
                Utils.registerError(holder, annotation.getNameReferenceElement(),
                        new AddAnnotationQuickFix(psiClass, Localization.message("class.quickfix.add-annotation", CbsDatamodelClass.SHORT_NAME), CbsDatamodelClass.SHORT_NAME),
                        Localization.message("field.warn.class-not-annotated", CbsAnnotation.CbsDatamodelClass.SHORT_NAME)
                );
            }
        }

        // Список с параметрами модификаций availableValues, объявленными в аннотации
        // Нужен для проверки того, что поля не объявлены одновременно
        List<PsiNameValuePair> availableValuesDeclaredPairs = new ArrayList<>();

        if (availableValuesPair != null){
            availableValuesDeclaredPairs.add(availableValuesPair);
        }

        // Проверка на то, что класс, указанный в availableValuesEnum - это енум
        if (availableValuesEnumPair != null && availableValuesEnumPair.getValue() instanceof PsiClassObjectAccessExpression enumType) {
            PsiType availableValuesEnumClass = enumType.getOperand().getType();
            PsiClass enumClass = PsiUtil.resolveClassInType(availableValuesEnumClass);

            if (enumClass == null || !enumClass.isEnum()) {
                Utils.registerError(holder, availableValuesEnumPair, Localization.message("field.warn.available-values-enum.must-be-enumeration", AVAILABLE_VALUES_ENUM));
            } else {
                availableValuesDeclaredPairs.add(availableValuesEnumPair);
            }
        }

        // Проверка на то, что класс, указанный в availableValuesEnumPath, существует и что это енум
        if (availableValuesEnumPathPair != null && availableValuesEnumPathPair.getValue() instanceof PsiLiteralExpression availableValuesEnumPathLiteral){
            if (availableValuesEnumPathLiteral.getValue() instanceof String availableValuesEnumPathLiteralString) {
                PsiClass enumClass = Utils.findPsiClassByQualifiedName(field.getProject(), availableValuesEnumPathLiteralString);
                if (enumClass == null){ // Класс не найден
                    Utils.registerError(holder, availableValuesEnumPathLiteral, Localization.message("field.warn.class-not-found"));
                } else if (enumClass.isEnum()) { // Класс найден, и это енум
                    availableValuesDeclaredPairs.add(availableValuesEnumPathPair);
                } else { // Класс найден, но это не енум
                    Utils.registerError(holder, availableValuesEnumPathLiteral, Localization.message("field.warn.available-values-enum-path.must-be-qname", AVAILABLE_VALUES_ENUM_PATH));
                }
            }
        }

        // Модификации параметра availableValues не могут быть объявлены вместе друг с другом
        if (availableValuesDeclaredPairs.size() == 3){ // Если объявлены все 3 модификации
            String availableValuesRedundantMsg = Localization.message("field.warn.redundant.availableValues-self", AVAILABLE_VALUES, AVAILABLE_VALUES_ENUM_PATH);
            String availableValuesEnumRedundantMsg = Localization.message("field.warn.redundant.availableValues-self", AVAILABLE_VALUES_ENUM, AVAILABLE_VALUES_ENUM_PATH);
            Utils.registerWarning(holder, availableValuesPair, getRemovePsiElementFix(availableValuesPair), availableValuesRedundantMsg);
            Utils.registerWarning(holder, availableValuesEnumPair, getRemovePsiElementFix(availableValuesEnumPair), availableValuesEnumRedundantMsg);
        }
        // Модификации параметра availableValues не могут быть объявлены вместе друг с другом
        else if (availableValuesDeclaredPairs.size() == 2){ // Если объявлены 2 из 3 модификаций
            PsiNameValuePair redundantParam = availableValuesDeclaredPairs.get(0);
            PsiNameValuePair hasPriorityParam = availableValuesDeclaredPairs.get(1);

            String errorMsg = Localization.message("field.warn.redundant.availableValues-self", redundantParam.getAttributeName(), hasPriorityParam.getAttributeName());
            Utils.registerWarning(holder, redundantParam, getRemovePsiElementFix(redundantParam), errorMsg);
        }

        // Самая приоритетная модификация availableValues
        PsiNameValuePair availableValuesPriorityPair = availableValuesDeclaredPairs.isEmpty() ? null : availableValuesDeclaredPairs.get(availableValuesDeclaredPairs.size() - 1);

        boolean maxLengthChecked = false; // Признак того, что maxLength уже проверено, чтобы выводить еще одно предупреждение
        // maxLength не может быть объявлено вместе с availableValues
        if (availableValuesPriorityPair != null && maxLengthPair != null){
            Utils.registerWarning(holder, maxLengthPair, getRemovePsiElementFix(maxLengthPair),
                    Localization.message("field.warn.redundant.availableValues-other", MAX_LENGTH, availableValuesPriorityPair.getAttributeName()));
            maxLengthChecked = true;
        }

        if (typePair != null && typePair.getValue() instanceof PsiClassObjectAccessExpression typeValue){
            PsiType mustType = typeValue.getOperand().getType();
            PsiClass mustTypeClass = PsiUtil.resolveClassInType(mustType);
            // Проверка на правильное указание типа
            if (mustTypeClass != null && !Utils.isClassOfAnyType(mustTypeClass, String.class, Long.class, Double.class, Boolean.class, Date.class)){
                Utils.registerError(holder, typePair, getReplaceTypeFix(typePair), Localization.message("field.warn.type.available-types", TYPE));
            }
            // Тип не может быть указан вместе с availableValues
            if (availableValuesPriorityPair != null){
                Utils.registerWarning(holder, typePair, getRemovePsiElementFix(typePair),
                        Localization.message("field.warn.redundant.availableValues-other", TYPE, availableValuesPriorityPair.getAttributeName()));
            }
            // Если указанный тип - Boolean
            if (mustTypeClass != null && Boolean.class.getTypeName().equals(mustTypeClass.getQualifiedName())){
                // Проверка на совместимость: type=Boolean не может быть объявлен вместе с maxLength
                if (maxLengthPair != null && !maxLengthChecked){
                    Utils.registerWarning(holder, maxLengthPair, getRemovePsiElementFix(maxLengthPair), Localization.message("field.warn.redundant.type-boolean", MAX_LENGTH));
                }
            }
        }

    }

    /**
     * Фикс с предложением заменить похожие классы на подходящие (доступные).<ul>
     *     <li>Character -> String</li>
     *     <li>Integer -> Long</li>
     *     <li>Float -> Double</li>
     *     <li>LocalDateTime, LocalDate, LocalTime -> Date</li>
     * </ul>
     */
    private ReplaceAnnotationParameterQuickFix getReplaceTypeFix(@NotNull PsiNameValuePair pair){
        if (!(pair.getParent() instanceof PsiAnnotationParameterList)) return null;
        if (!(pair.getValue() instanceof PsiClassObjectAccessExpression typeValue)) return null;

        PsiType currentType = typeValue.getOperand().getType();
        PsiClass currentTypeClass = PsiUtil.resolveClassInType(currentType);
        if (currentTypeClass == null) return null;

        Class<?> offeredType = null;
        if (Utils.isClassOfAnyType(currentTypeClass, Character.class)){
            offeredType = String.class;
        } else if (Utils.isClassOfAnyType(currentTypeClass, Integer.class)) {
            offeredType = Long.class;
        } else if (Utils.isClassOfAnyType(currentTypeClass, Float.class)) {
            offeredType = Double.class;
        } else if (Utils.isClassOfAnyType(currentTypeClass, LocalDateTime.class, LocalDate.class, LocalTime.class)) {
            offeredType = Date.class;
        }

        if (offeredType == null) return null;
        return new ReplaceAnnotationParameterQuickFix(pair, offeredType.getSimpleName() + ".class");
    }

}
