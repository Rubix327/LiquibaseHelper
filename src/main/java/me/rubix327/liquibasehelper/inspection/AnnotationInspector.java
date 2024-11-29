package me.rubix327.liquibasehelper.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.PsiUtil;
import me.rubix327.liquibasehelper.Utils;
import me.rubix327.liquibasehelper.log.MainLogger;
import me.rubix327.liquibasehelper.settings.CbsAnnotation;
import me.rubix327.liquibasehelper.settings.StaticSettings;
import org.jetbrains.annotations.NotNull;

import java.util.*;

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
                PsiAnnotation annotation = Utils.findAnnotation(psiClass, CbsAnnotation.CbsDatamodelClass.INSTANCE);
                if (annotation == null) return;

                // Проверяем на внутренний класс и не обновляем его
                if (psiClass.getContainingClass() != null){
                    Utils.registerError(holder, annotation, "<html><b>@%s</b> is not supported on inner classes.</html>", CbsAnnotation.CbsDatamodelClass.SHORT_NAME);
                    return;
                }

                // Не обновляем класс, если его название не совпадает с названием файла, в котором он находится
                if (Utils.isClassAndFileNamesNotMatch(psiClass)){
                    Utils.registerError(holder, annotation, "<html>This class will not be loaded as <b>@%s</b> unless its name matches the file name.</html>", CbsAnnotation.CbsDatamodelClass.SHORT_NAME);
                    return;
                }

                String thisClassQualifiedName = psiClass.getQualifiedName();

                // Удаляем мусорные правила в процессе изменения value у аннотации @CbsDatamodelClass
                String expectedDatamodelName = instance.getDatamodelValueFromRegistry(thisClassQualifiedName);
                String realDatamodelName = RulesManager.getDatamodelNameOfClass(psiClass);
                if (realDatamodelName != null && !realDatamodelName.equals(expectedDatamodelName) && thisClassQualifiedName != null){
                    instance.removeRulesByTagName(expectedDatamodelName);
                }

                // Проверка на дублирующие значения CbsDatamodelClass.value у разных классов
                if (realDatamodelName != null){
                    List<String> classes = instance.getClassesByDatamodelName(realDatamodelName); // Получаем все классы, использующие этот datamodelName
                    if (classes.size() > 1){
                        classes.remove(thisClassQualifiedName); // Удаляем из списка текущий класс, чтобы оставить только другие
                        String anotherClass = classes.get(0); // Получаем первый попавшийся класс
                        instance.removeRulesByTagName(realDatamodelName); // Удаляем все правила этого datamodelName
                        Utils.registerError(holder, annotation,
                                "<html>Value <b>%s</b> is already used by class %s." +
                                "<br>These classes will not be loaded unless you change the value on one of them.</html>",
                                realDatamodelName, Utils.getHtmlLink(anotherClass, anotherClass));
                        return;
                    }
                }

                // Собираем правила для класса и его родителей
                instance.handleClassAndSuperClasses(psiClass);

                // Обновляем правила наследников, в случае если были изменены правила в родителе
                for (PsiClass inheritor : getInheritors(psiClass, ".metaloader.")) {
                    instance.handleClassAndSuperClasses(inheritor);
                }

                super.visitClass(psiClass);
            }

            void updateClassesUsingEnums(@NotNull RulesManager instance, @NotNull PsiClass psiClass){
                // Если обновляется enum, который используется в мета-классах, то нужно обновить правила в этих мета-классах
                Set<PsiClass> classesToUpdate = instance.getClassesUsingThisEnum(psiClass);
                if (classesToUpdate != null){
                    MainLogger.info(instance.getProject(), "Classes using enum %s: %s", psiClass.getName(), classesToUpdate);
                    for (PsiClass classToUpdate : classesToUpdate) {
                        if (classToUpdate == null || !classToUpdate.isValid()) continue;
                        // TODO нужно также обновлять дочерние классы
                        instance.handleClassAndSuperClasses(classToUpdate);
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
        // TODO filter by @CbsDatamodelClass, and not @CbsDatamodelMappedSuperclass
        return ClassInheritorsSearch.search(psiClass, scope, true)
                .findAll().stream()
                .filter(c -> c.getQualifiedName() != null)
                .filter(c -> c.getQualifiedName().contains(mustContainPath))
                .toList();
    }

    public void checkForFieldConstraints(PsiField field, ProblemsHolder holder) {
        PsiAnnotation annotation = Utils.findAnnotation(field, CbsAnnotation.CbsDatamodelField.INSTANCE);
        if (annotation == null){
            return;
        }

        PsiAnnotationMemberValue availableValuesMember = annotation.findDeclaredAttributeValue(AVAILABLE_VALUES);
        PsiAnnotationMemberValue availableValuesEnumMember = annotation.findDeclaredAttributeValue(AVAILABLE_VALUES_ENUM);
        PsiAnnotationMemberValue availableValuesEnumPath = annotation.findDeclaredAttributeValue(AVAILABLE_VALUES_ENUM_PATH);
        PsiAnnotationMemberValue type = annotation.findDeclaredAttributeValue(CbsAnnotation.CbsDatamodelField.Fields.TYPE);

        PsiClass psiClass = field.getContainingClass();
        if (psiClass != null){
            if (Utils.findAnnotation(psiClass, CbsAnnotation.CbsDatamodelClass.INSTANCE) == null){
                Utils.registerError(holder, annotation, "This field won't be checked since the class is not annotated with @%s.", CbsAnnotation.CbsDatamodelClass.SHORT_NAME);
            }
        }

        // Список с названиями полей availableValues, объявленными в аннотации
        // Нужен для проверки того, что поля не объявлены одновременно
        List<String> availableValuesDeclaredParams = new ArrayList<>();

        if (availableValuesMember != null){
            availableValuesDeclaredParams.add(AVAILABLE_VALUES);
        }

        // Проверка на то, что класс в availableValuesEnum - это енум
        if (availableValuesEnumMember instanceof PsiClassObjectAccessExpression enumType) {
            PsiType availableValuesEnumClass = enumType.getOperand().getType();
            PsiClass enumClass = PsiUtil.resolveClassInType(availableValuesEnumClass);

            if (enumClass == null || !enumClass.isEnum()) {
                Utils.registerError(holder, availableValuesEnumMember, "<html>Value of <b>%s</b> must be an enumeration.</html>", AVAILABLE_VALUES_ENUM);
            } else {
                availableValuesDeclaredParams.add(AVAILABLE_VALUES_ENUM);
            }
        }

        // Проверка на то, что класс в availableValuesEnumPath существует и что это енум
        if (availableValuesEnumPath instanceof PsiLiteralExpression availableValuesEnumPathLiteral){
            if (availableValuesEnumPathLiteral.getValue() instanceof String availableValuesEnumPathLiteralString) {
                PsiClass enumClass = Utils.findPsiClassByQualifiedName(field.getProject(), availableValuesEnumPathLiteralString);
                if (enumClass == null){ // Класс не найден
                    Utils.registerError(holder, availableValuesEnumPathLiteral, "Class not found.");
                } else if (enumClass.isEnum()) { // Класс найден, и это енум
                    availableValuesDeclaredParams.add(AVAILABLE_VALUES_ENUM_PATH);
                } else { // Класс найден, но это не енум
                    Utils.registerError(holder, availableValuesEnumPathLiteral, "<html>Value of <b>%s</b> must be a qualified name of an enumeration.</html>", AVAILABLE_VALUES_ENUM_PATH);
                }
            }
        }

        // Проверка на то, что availableValues, availableValuesEnum и availableValuesEnumPath не заполнены одновременно
        if (availableValuesDeclaredParams.size() > 1) {
            StringBuilder errorBuilder = new StringBuilder("<html>Fields ");
            String errorFields = String.join(" and ", availableValuesDeclaredParams.stream().map(e -> "<b>" + e + "</b>").toList());
            errorBuilder.append(errorFields).append(" must not be declared at the same time.</html>");
            Utils.registerError(holder, annotation, errorBuilder.toString());
            return;
        }

        // Проверка на правильное указание типа
        if (type instanceof PsiClassObjectAccessExpression typeValue){
            PsiType mustType = typeValue.getOperand().getType();
            PsiClass mustTypeClass = PsiUtil.resolveClassInType(mustType);
            if (mustTypeClass != null && !isClassOfAnyType(mustTypeClass, String.class, Long.class, Boolean.class, Date.class)){
                Utils.registerError(holder, type, "<html>Field <b>%s</b> must be one of the following: [String.class, Long.class, Boolean.class, Date.class]</html>", TYPE);
            }
            if (mustTypeClass != null && Boolean.class.getTypeName().equals(mustTypeClass.getQualifiedName())){
                if (!availableValuesDeclaredParams.isEmpty()){
                    Utils.registerError(holder, annotation, "<html>Fields <b>type=Boolean.class</b> and %s must not be declared at the same time.</html>", availableValuesDeclaredParams.get(0));
                }
            }
        }
    }

    public static boolean isClassOfAnyType(@NotNull PsiClass psiClass, @NotNull Class<?>... types){
        return Arrays.stream(types).map(Class::getName).anyMatch(t -> t.equals(psiClass.getQualifiedName()));
    }

}
