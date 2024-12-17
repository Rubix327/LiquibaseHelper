package me.rubix327.liquibasehelper.inspection;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtil;
import me.rubix327.liquibasehelper.Utils;
import me.rubix327.liquibasehelper.inspection.model.AvailableValue;
import me.rubix327.liquibasehelper.inspection.model.HandleClassesResponse;
import me.rubix327.liquibasehelper.inspection.model.TagRule;
import me.rubix327.liquibasehelper.inspection.model.TagRulesContainer;
import me.rubix327.liquibasehelper.log.MainLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static me.rubix327.liquibasehelper.settings.CbsAnnotation.*;

public class RulesManager {

    /**
     * Экземпляры RulesManager. На каждый проект - свой экземпляр.
     */
    private static final Map<String, RulesManager> instances = new HashMap<>();
    /**
     * Регистр правил, привязанных к datamodelName.<br>
     * Нужен для быстрого нахождения списка правил определенного тега во время парсинга xml-файла.
     */
    private final Map<String, TagRulesContainer> parentToTagRulesContainer = new HashMap<>(); // "accIntentionTreeMeta" -> TagRulesContainer
    /**
     * Регистр классов-перечислений к классам @CbsDatamodelClass, которые их используют.<br>
     * Нужен для обновления правил после изменения enum-а.
     */
    private final Map<PsiClass, Set<PsiClass>> enumsToClassesUsingThem = new HashMap<>(); // changed class -> classes to update
    /**
     * Регистр qualifiedName -> datamodelName.<br>
     * Нужен для удаления мусорных правил во время изменения value у @CbsDatamodelClass
     */
    private final Map<String, String> classToDatamodelValueRegistry = new HashMap<>(); // "me.rubix327.AccIntentionTreeMeta" -> "accIntentionTreeMeta"

    @NotNull
    private final Project project;

    private RulesManager(@NotNull Project project) {
        this.project = project;
        instances.put(project.getBasePath(), this);
    }

    public static RulesManager getInstance(Project project){
        RulesManager existingInstance = instances.get(project.getBasePath());
        return Objects.requireNonNullElseGet(existingInstance, () -> new RulesManager(project));
    }

    public static Collection<RulesManager> getAllInstances(){
        return instances.values();
    }

    @NotNull
    public Project getProject() {
        return project;
    }

    /**
     * Сбросить все правила для этого проекта.
     */
    public void resetAll(){
        parentToTagRulesContainer.clear();
        enumsToClassesUsingThem.clear();
        classToDatamodelValueRegistry.clear();
    }

    /**
     * Обновить все правила для указанного класса и его родителей.
     */
    public HandleClassesResponse handleClassAndSuperClasses(@NotNull PsiClass psiClass) {
        // Если класс == null или у него нет аннотации @CbsDatamodelClass
        if (Utils.isNotDatamodelClass(psiClass)){
            return HandleClassesResponse.makeErrorResponse(psiClass, "Skipping class {class} (not a datamodel class).");
        }
        // Если класс mapped, то пропускаем (такой класс только встраивает свои правила внутрь дочерних)
        if (Utils.isDatamodelMappedClass(psiClass)){
            return HandleClassesResponse.makeErrorResponse(psiClass, "Skipping class {class} (the class is mapped).");
        }
        // Если класс вложенный
        if (Utils.isClassAndFileNamesNotMatch(psiClass)) {
            return HandleClassesResponse.makeErrorResponse(psiClass, "Skipping class {class} (the class is inner).");
        }

        List<TagRule> rulesFromClass = getRulesFromFields(psiClass);
        assert rulesFromClass != null; // Все проверки уже проведены выше, поэтому метод getRulesFromFields не может вернуть null

        List<TagRule> rulesFromClassAndSuperClasses = getRulesFromSuperClasses(psiClass, rulesFromClass);
        String datamodelNameOfClass = getDatamodelNameOfClass(psiClass);

        if (psiClass.getQualifiedName() != null && datamodelNameOfClass != null){
            putDatamodelValueToRegistry(psiClass.getQualifiedName(), datamodelNameOfClass);
        }

        parentToTagRulesContainer.put(datamodelNameOfClass, new TagRulesContainer()
                .setParentTagName(datamodelNameOfClass)
                .setTagRules(rulesFromClassAndSuperClasses)
                .setParentTagTooltip(Utils.getCbsDatamodelClassAnnotationFieldValue(psiClass, CbsDatamodelClass.Fields.COMMENT))
                .setParentTagDescription(Utils.getCbsDatamodelClassAnnotationFieldValue(psiClass, CbsDatamodelClass.Fields.DESCRIPTION))
                .setLinkToMetaClass(psiClass.getQualifiedName())
                .setLinkToClassNameOffset(psiClass.getTextOffset())
        );

        return new HandleClassesResponse(psiClass).setSuccess(true)
                .setMessage("Registered rules for class {class}: %s (base: %s, super: %s)",
                        rulesFromClassAndSuperClasses.size(), rulesFromClass.size(),
                        rulesFromClassAndSuperClasses.size() - rulesFromClass.size());
    }

    /**
     * Получить правила из родителей указанного класса.<br>
     * Если правило уже существует в списке, то от следующего родителя оно добавлено не будет.<br><br>
     * <b>Рекурсивный метод.</b>
     * @param baseClass Класс
     * @param existingRules Уже сформированные правила
     * @return Список правил от всех родителей
     */
    @NotNull
    private List<TagRule> getRulesFromSuperClasses(@NotNull PsiClass baseClass, @NotNull List<TagRule> existingRules) {
        PsiClass superClass = baseClass.getSuperClass();
        List<TagRule> rulesFromSuper = getRulesFromFields(superClass);

        // Если родительский класс не подходит, то возвращаем сформированные правила (конец рекурсии)
        if (rulesFromSuper == null){
            return existingRules;
        }

        List<TagRule> mergedRules = mergeRules(existingRules, rulesFromSuper);
        assert baseClass.getSuperClass() != null;
        return getRulesFromSuperClasses(baseClass.getSuperClass(), mergedRules);
    }

    @NotNull
    private List<TagRule> mergeRules(@NotNull List<TagRule> rulesFromBase, @NotNull List<TagRule> rulesFromSuper) {
        List<TagRule> mergedRules = new ArrayList<>(rulesFromBase);
        for (TagRule ruleFromSuper : rulesFromSuper) {
            // Если в правилах дочерних классов уже есть этот тег, то из родителя его не добавляем
            if (rulesFromBase.stream().anyMatch(e -> e.getTagName().equals(ruleFromSuper.getTagName()))) {
                continue;
            }
            // Правила из родителя вставляем в начало, чтобы в документации они были спереди
            mergedRules.add(0, ruleFromSuper);
        }
        return mergedRules;
    }

    /**
     * Получить мета-название класса - либо через обычное название класса, либо через value аннотации @CbsDatamodelClass.
     * @param psiClass Класс
     * @return Мета-название (такое название должно быть у родительского тега, чтобы правило сработало)
     */
    @Nullable
    public static String getDatamodelNameOfClass(@NotNull PsiClass psiClass){
        // Из tag в аннотации @CbsDatamodelClass
        String fromField = Utils.getCbsDatamodelClassAnnotationFieldValue(psiClass, CbsDatamodelClass.Fields.TAG);
        if (fromField != null){
            return fromField;
        }

        if (psiClass.getName() == null) return null;

        // Из названия класса (приводим к lowerCamelCase)
        if (psiClass.getName().length() == 1){
            // Если в названии один символ
            return psiClass.getName().toLowerCase();
        } else {
            // Если в названии несколько символов
            return Character.toLowerCase(psiClass.getName().charAt(0)) + psiClass.getName().substring(1);
        }
    }

    /**
     * Получить все правила из полей указанного класса.
     * @param psiClass Класс
     * @return Правила из полей
     */
    private List<TagRule> getRulesFromFields(@Nullable PsiClass psiClass) {
        if (Utils.isNotDatamodelClass(psiClass)) {
            return null;
        }

        assert psiClass != null;
        // Название класса не совпадает с названием файла, в котором он находится (это вложенный класс)
        if (Utils.isClassAndFileNamesNotMatch(psiClass)) {
            return null;
        }

        PsiField[] fields = psiClass.getFields();
        List<TagRule> resultRules = new ArrayList<>();

        for (PsiField field : fields) {
            PsiAnnotation fieldAnnotation = Utils.findAnnotation(field, CbsDatamodelField.INSTANCE);
            if (fieldAnnotation == null) {
                continue;
            }

            TagRule tagRule = new TagRule(field.getName());
            PsiAnnotationMemberValue tooltipText = fieldAnnotation.findDeclaredAttributeValue(CbsDatamodelField.Fields.COMMENT);
            PsiAnnotationMemberValue tooltipDescription = fieldAnnotation.findDeclaredAttributeValue(CbsDatamodelField.Fields.DESCRIPTION);
            PsiAnnotationMemberValue required = fieldAnnotation.findDeclaredAttributeValue(CbsDatamodelField.Fields.REQUIRED);
            PsiAnnotationMemberValue maxLength = fieldAnnotation.findDeclaredAttributeValue(CbsDatamodelField.Fields.MAX_LENGTH);
            PsiAnnotationMemberValue availableValues = fieldAnnotation.findDeclaredAttributeValue(CbsDatamodelField.Fields.AVAILABLE_VALUES);
            PsiAnnotationMemberValue availableValuesEnum = fieldAnnotation.findDeclaredAttributeValue(CbsDatamodelField.Fields.AVAILABLE_VALUES_ENUM);
            PsiAnnotationMemberValue availableValuesEnumPath = fieldAnnotation.findDeclaredAttributeValue(CbsDatamodelField.Fields.AVAILABLE_VALUES_ENUM_PATH);
            PsiAnnotationMemberValue type = fieldAnnotation.findDeclaredAttributeValue(CbsDatamodelField.Fields.TYPE);

            tagRule.setMetaClassPath(psiClass.getQualifiedName());
            tagRule.setMetaFieldOffset(field.getTextOffset());

            if (tooltipText instanceof PsiLiteralExpression tooltipString){
                if (tooltipString.getValue() instanceof String tooltipValue && Utils.isNotBlank(tooltipValue)){
                    tagRule.setTagTooltip(tooltipValue);
                }
            }

            if (tooltipDescription instanceof PsiLiteralExpression tooltipDescriptionString){
                if (tooltipDescriptionString.getValue() instanceof String tooltipDescriptionValue && Utils.isNotBlank(tooltipDescriptionValue)){
                    tagRule.setTagDescription(tooltipDescriptionValue);
                }
            }

            if (required instanceof PsiLiteralExpression requiredBoolean){
                if (requiredBoolean.getValue() instanceof Boolean requiredBooleanValue){
                    tagRule.setRequired(requiredBooleanValue);
                }
            }

            if (maxLength instanceof PsiLiteralExpression maxLengthInteger){
                if (maxLengthInteger.getValue() instanceof Integer maxLengthIntegerValue){
                    tagRule.setMaxLength(maxLengthIntegerValue);
                }
            }

            if (type instanceof PsiClassObjectAccessExpression typeValue){
                PsiType mustType = typeValue.getOperand().getType();
                PsiClass mustTypeClass = PsiUtil.resolveClassInType(mustType);
                if (mustTypeClass != null && AnnotationInspector.isClassOfAnyType(mustTypeClass, String.class, Long.class, Double.class, Boolean.class, Date.class)){
                    tagRule.setType(mustTypeClass.getQualifiedName());
                }
            }

            // Если тип Boolean, то заполнять возможные значения не требуется
            if (Boolean.class.getTypeName().equals(tagRule.getType())){
                resultRules.add(tagRule);
                continue;
            }

            // Возможные значения из availableValues в виде одиночной строки
            if (availableValues instanceof PsiLiteralExpression availableValuesString){
                if (availableValuesString.getValue() instanceof String availableValuesStringString){
                    tagRule.setAvailableValues(AvailableValue.stringToAvailableValues(availableValuesStringString));
                }
            }

            // Возможные значения из availableValues в виде массива
            if (availableValues instanceof PsiArrayInitializerMemberValue availableValuesArray){
                List<AvailableValue> availableValuesList = new ArrayList<>();

                for (PsiAnnotationMemberValue initializer : availableValuesArray.getInitializers()) {
                    if (initializer instanceof PsiLiteralExpression initializerString){
                        if (initializerString.getValue() instanceof String s){
                            availableValuesList.addAll(AvailableValue.stringToAvailableValues(s));
                        }
                    }
                }

                tagRule.setAvailableValues(availableValuesList);
            }

            // Возможные значения из availableValuesEnum
            List<AvailableValue> availableValuesFromEnum = getAvailableValuesFromAnnotationEnum(availableValuesEnum, psiClass);
            if (availableValuesFromEnum != null && !availableValuesFromEnum.isEmpty()){
                tagRule.setAvailableValues(availableValuesFromEnum);
            }

            // Возможные значения из availableValuesEnumPath
            if (availableValuesEnumPath instanceof PsiLiteralExpression availableValuesEnumPathLiteral){
                if (availableValuesEnumPathLiteral.getValue() instanceof String availableValuesEnumPathLiteralString){
                    PsiClass enumClass = Utils.findPsiClassByQualifiedName(project, availableValuesEnumPathLiteralString);
                    List<AvailableValue> availableValuesFromEnumPath = getAvailableValuesFromEnum(enumClass, psiClass);
                    if (availableValuesFromEnumPath != null){
                        tagRule.setAvailableValues(availableValuesFromEnumPath);
                    }
                }
            }

            resultRules.add(tagRule);
        }

        return resultRules;
    }

    /**
     * Получить значения из класса перечислимого типа.
     * Приоритет получения см. в {@link #getEnumValue(PsiEnumConstant, PsiClass)}.
     * @param annotationMemberValue Перечислимый тип
     * @param psiClass Класс
     * @return Список значений перечислимого типа
     * @see #getEnumValue(PsiEnumConstant, PsiClass)
     */
    private List<AvailableValue> getAvailableValuesFromAnnotationEnum(PsiAnnotationMemberValue annotationMemberValue, PsiClass psiClass){
        if (annotationMemberValue instanceof PsiClassObjectAccessExpression annotationMember){
            PsiType type = annotationMember.getOperand().getType();
            PsiClass enumClass = PsiUtil.resolveClassInType(type);
            return getAvailableValuesFromEnum(enumClass, psiClass);
        }
        return null;
    }

    @Nullable
    private List<AvailableValue> getAvailableValuesFromEnum(PsiClass enumClass, @NotNull PsiClass searchingFromClass){
        List<AvailableValue> result = null;
        if (enumClass != null && enumClass.isEnum()) {
            // Класс, в котором используется этот енум, нужно обновлять в случае обновления самого енума
            addEnumToClassesUsingIt(enumClass, searchingFromClass);

            result = new ArrayList<>();
            for (PsiField field : enumClass.getFields()) {
                if (Utils.findAnnotation(field, CbsDatamodelIgnore.INSTANCE) != null) continue;
                if (!(field instanceof PsiEnumConstant enumConstant)) continue;

                String value = getEnumValue(enumConstant, enumClass);
                String comment = Utils.getFirstMeaningfulLineFromDocComment(field);
                result.add(new AvailableValue(value, comment));
            }
        }
        return result;
    }

    /**
     * Добавить класс перечислимого типа в регистр {@link #enumsToClassesUsingThem}.
     * @param changedClass Перечислимый тип
     * @param classToUpdate Класс, использующий этот ПТ
     */
    private void addEnumToClassesUsingIt(PsiClass changedClass, PsiClass classToUpdate){
        Set<PsiClass> existingClassesToUpdate = enumsToClassesUsingThem.get(changedClass);
        if (existingClassesToUpdate == null){
            enumsToClassesUsingThem.put(changedClass, new HashSet<>(Collections.singletonList(classToUpdate)));
        } else {
            existingClassesToUpdate.add(classToUpdate);
        }
    }

    /**
     * Получить значение поля перечислимого типа в следующем приоритете:<ol>
     *   <li>Значение value из @CbsDatamodelValue(value)</li>
     *   <li>Значение из поля value, объявленного внутри перечислимого типа (value обязательно должно объявляться через конструктор)</li>
     *   <li>Название перечислимого типа, без каких-либо модификаций</li>
     * </ol>
     * @param field Поле
     * @param enumClass Класс перечислимого типа
     * @return Значение
     */
    private String getEnumValue(PsiEnumConstant field, PsiClass enumClass){
        PsiAnnotation valueAnnotation = Utils.findAnnotation(field, CbsDatamodelValue.INSTANCE);
        if (valueAnnotation != null){
            PsiAnnotationMemberValue value = valueAnnotation.findDeclaredAttributeValue(CbsDatamodelValue.Fields.VALUE);
            if (value instanceof PsiLiteralExpression psiValue){
                return (String) psiValue.getValue();
            }
        }

        Integer valueIndexInConstructor = getValueIndexInConstructor(enumClass);
        PsiExpressionList enumArgumentList = field.getArgumentList();
        if (valueIndexInConstructor != null && enumArgumentList != null && enumArgumentList.getExpressions().length >= valueIndexInConstructor + 1){
            PsiExpression[] expressions = enumArgumentList.getExpressions();
            PsiExpression firstArg = expressions[valueIndexInConstructor];
            return firstArg.getText().replace("\"", "");
        }

        return field.getName();
    }

    /**
     * Получить индекс поля value в конструкторе класса.
     * @param enumClass Класс
     * @return Индекс поля value
     */
    private Integer getValueIndexInConstructor(PsiClass enumClass) {
        if (enumClass == null || !enumClass.isEnum()) {
            return null; // Это не enum
        }

        // Проверяем наличие поля value
        boolean hasValueField = false;
        for (PsiField field : enumClass.getFields()) {
            if (field.getName().equals("value")) {
                hasValueField = true;
                break;
            }
        }

        if (!hasValueField){
            return null;
        }

        for (PsiMethod method : enumClass.getMethods()) {
            if (method.isConstructor()) {
                PsiParameter[] parameters = method.getParameterList().getParameters();
                for (int i = 0; i < parameters.length; i++) {
                    if ("value".equals(parameters[i].getName())){
                        return i;
                    }
                }
            }
        }

        return null;
    }

    public void removeRulesOfClass(@NotNull PsiClass psiClass){
        String datamodelName = getDatamodelNameOfClass(psiClass);
        parentToTagRulesContainer.remove(datamodelName);
        MainLogger.info(psiClass.getProject(), "Удалены правила для тега %s (класс: %s)", datamodelName, psiClass.getName());
    }

    public void removeRulesByTagName(String tagName){
        parentToTagRulesContainer.remove(tagName);
    }

    public TagRulesContainer getRulesContainerByTagName(String tagName){
        return parentToTagRulesContainer.get(tagName);
    }

    public Collection<TagRulesContainer> getAllRulesContainers(){
        return parentToTagRulesContainer.values();
    }

    @SuppressWarnings("unused")
    public static void printAllProjectsRules(){
        MainLogger.info("---------- All Rules ----------");
        instances.values().forEach(inst -> MainLogger.info("%s -> %s", inst.getProject().getName(), inst.parentToTagRulesContainer.values()));
        MainLogger.info("--------------------------------");
    }

    @SuppressWarnings("unused")
    public void printAllRules(){
        MainLogger.info(project, "---------- Rules ----------");
        for (Map.Entry<String, TagRulesContainer> stringListEntry : parentToTagRulesContainer.entrySet()) {
            MainLogger.info(project, "%s -> %s", stringListEntry.getKey(), stringListEntry.getValue());
        }
        MainLogger.info(project, "--------------------------");
    }

    public Set<PsiClass> getClassesUsingThisEnum(PsiClass usedEnum){
        return enumsToClassesUsingThem.get(usedEnum);
    }

    public void removeClassReferencesFromEnums(PsiClass removedClass){
        for (Map.Entry<PsiClass, Set<PsiClass>> enumsToClasses : enumsToClassesUsingThem.entrySet()) {
            enumsToClasses.getValue().remove(removedClass);
        }
    }

    public String getDatamodelValueFromRegistry(String classQualifiedName){
        return classToDatamodelValueRegistry.get(classQualifiedName);
    }

    public void removeDatamodelValueFromRegistry(String classQualifiedName){
        classToDatamodelValueRegistry.remove(classQualifiedName);
    }

    public void putDatamodelValueToRegistry(@NotNull String classQualifiedName, @NotNull String datamodelName){
        classToDatamodelValueRegistry.put(classQualifiedName, datamodelName);
    }

    @SuppressWarnings("unused")
    public void printAllClassesToDatamodelRegistry(){
        MainLogger.info(project, "------- Datamodel Registry ------");
        for (Map.Entry<String, String> stringStringEntry : classToDatamodelValueRegistry.entrySet()) {
            MainLogger.info(project, "%s -> %s", stringStringEntry.getKey(), stringStringEntry.getValue());
        }
        MainLogger.info(project, "---------------------------------");
    }

    public List<String> getClassesByDatamodelName(@NotNull String datamodelName){
        List<String> classes = new ArrayList<>();
        for (Map.Entry<String, String> stringStringEntry : classToDatamodelValueRegistry.entrySet()) {
            if (stringStringEntry.getValue().equals(datamodelName)){
                classes.add(stringStringEntry.getKey());
            }
        }
        return classes;
    }

}
