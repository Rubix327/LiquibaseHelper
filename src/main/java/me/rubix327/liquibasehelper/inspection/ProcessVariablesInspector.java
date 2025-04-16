package me.rubix327.liquibasehelper.inspection;

import com.intellij.codeInspection.*;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import me.rubix327.liquibasehelper.Utils;
import me.rubix327.liquibasehelper.inspection.model.ProcessVariablesKeys;
import me.rubix327.liquibasehelper.inspection.quickfix.OpenPsiElementQuickFix;
import me.rubix327.liquibasehelper.locale.Localization;
import me.rubix327.liquibasehelper.log.MainLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ProcessVariablesInspector extends LocalInspectionTool {

    private static final String CBS_ABSTRACT_WORKER_PATH = "ru.athena.cbs.processengine.client.worker.CbsAbstractWorker";
    private static final String PROCESS_VARIABLES_PATH = "ru.athena.cbs.processengine.entity.ProcessVariables";

    private SmartPsiElementPointer<PsiClass> cbsWorkerClass;

    private PsiClass findCbsWorkerClassPointer(PsiClass psiClass) {
        if (cbsWorkerClass != null){
            return cbsWorkerClass.getElement();
        }

        PsiClass workerClass = JavaPsiFacade.getInstance(psiClass.getProject()).findClass(
                CBS_ABSTRACT_WORKER_PATH,
                psiClass.getResolveScope()
        );
        if (workerClass != null){
            this.cbsWorkerClass = SmartPointerManager.createPointer(workerClass);
        }

        return workerClass;
    }

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @Override
            public void visitClass(@NotNull PsiClass psiClass) {
                super.visitClass(psiClass);

                // Проверка на то, что текущий класс является воркером (наследником от CbsWorker)
                if (isClassNotWorker(psiClass)) return;

                // Ищем метод List<ProcessVariableDefinition> defineVariables
                PsiMethod defineVariablesMethod = getDefineVariablesMethod(psiClass, 0);
                if (defineVariablesMethod == null) return; // Метод не определен

                // Извлекаем все ключи из метода defineVariables
                ProcessVariablesKeys keys = parseDefineVariables(psiClass, defineVariablesMethod);
                MainLogger.info(psiClass.getProject(), "Worker %s process variables keys: %s", psiClass.getName(), keys.toShortString());

                psiClass.accept(new PsiRecursiveElementVisitor() {
                    @Override
                    public void visitElement(@NotNull PsiElement element) {
                        super.visitElement(element);

                        if (element instanceof PsiMethodCallExpression methodCallExpression){
                            checkMethodCallExpression(keys, methodCallExpression, new OpenPsiElementQuickFix(defineVariablesMethod,
                                    Localization.message("processVariables.quickfix.open-define-variables"))
                            );
                        }
                    }
                });
            }

            private void checkMethodCallExpression(@NotNull ProcessVariablesKeys keys, @NotNull PsiMethodCallExpression expression, @NotNull LocalQuickFix quickFix){
                // Проверка на то, что используются только методы get или set
                String methodName = expression.getMethodExpression().getReferenceName();
                if (!List.of("get", "put").contains(methodName)) return;

                // Проверка на то, что метод get или put вызывается от объекта класса ProcessVariables
                PsiExpression qualifier = expression.getMethodExpression().getQualifierExpression();
                if (qualifier == null) return;
                if (qualifier.getType() == null || !qualifier.getType().equalsToText(PROCESS_VARIABLES_PATH)) return;

                // Извлекаем переданный ключ (первый аргумент метода)
                var argumentList = expression.getArgumentList().getExpressions();
                if (argumentList.length == 0) return;
                var keyExpression = argumentList[0];

                // Получаем значение ключа
                String key = resolveStringValue(keyExpression);
                if (key == null) return; // Не удалось определить значение

                // Проверка на то, чтобы текущая переменная была объявлена внутри defineVariables с подходящим направлением
                if ("get".equals(methodName) && !keys.hasInput(key)){
                    Utils.registerWarning(holder, keyExpression, quickFix, Localization.message("processVariables.warn.value-not-defined", key, "INPUT"));
                }
                else if ("put".equals(methodName) && !keys.hasOutput(key)){
                    Utils.registerWarning(holder, keyExpression, quickFix, Localization.message("processVariables.warn.value-not-defined", key, "OUTPUT"));
                }
            }

        };

    }

    /**
     * Является ли указанный класс воркером
     */
    private boolean isClassNotWorker(@NotNull PsiClass psiClass){
        // Проверка на то, что текущий класс является воркером (наследником от CbsWorker)
        if (psiClass.getSuperClass() == null) return true;
        PsiClass cbsWorkerClass = findCbsWorkerClassPointer(psiClass);
        if (cbsWorkerClass == null) return true;
        return !psiClass.isInheritor(cbsWorkerClass, true);
    }

    /**
     * Найти метод defineVariables в текущем классе или родителях текущего класса
     */
    @Nullable
    private PsiMethod getDefineVariablesMethod(@NotNull PsiClass containingClass, int triesCounter){
        for (PsiMethod method : containingClass.getMethods()) {
            if (method.getReturnType() == null) continue;
            if (!"List<ProcessVariableDefinition>".equals(method.getReturnType().getPresentableText())) continue;
            if (!"defineVariables".equals(method.getName())) continue;
            if (method.hasModifierProperty(PsiModifier.ABSTRACT)) continue; // Метод абстрактный

            return method;
        }

        // Если не нашли в текущем классе, то ищем в его родителе (максимальная глубина = 5)
        if (containingClass.getSuperClass() != null && triesCounter < 5){
            return getDefineVariablesMethod(containingClass.getSuperClass(), ++triesCounter);
        }

        return null;
    }

    /**
     * Извлечь ключи из метода defineVariables.
     * Идем последовательно от выражения return к объявлению списка и парсим объявления ProcessVariablesDefinitions.
     */
    @NotNull
    private ProcessVariablesKeys parseDefineVariables(@NotNull PsiClass containingClass, @NotNull PsiMethod method) {
        ProcessVariablesKeys keys = new ProcessVariablesKeys();

        // Извлекаем тело метода
        PsiCodeBlock body = method.getBody();
        if (body == null) return keys;

        // Ищем все выражения return
        List<PsiReturnStatement> returnStatements = new ArrayList<>();
        for (PsiStatement statement : body.getStatements()) {
            if (!(statement instanceof PsiReturnStatement returnStatement)) continue;
            returnStatements.add(returnStatement);
        }

        // Если выражений return 0 или больше 1, то ничего не делаем
        if (returnStatements.size() != 1) return keys;

        // Парсим выражение return
        PsiExpression returnValue = returnStatements.get(0).getReturnValue();
        if (returnValue != null) {
            // Если значение return это ссылка на другую переменную
            if (returnValue instanceof PsiReferenceExpression referenceExpression) {
                return extractFromReference(containingClass, referenceExpression);
            }
            // Если значение return это вызов метода
            if (returnValue instanceof PsiMethodCallExpression methodCall) {
                return extractFromMethodCall(containingClass, methodCall, null);
            }
            // Упускаем ситуацию, где в return возвращается выражение 'new'
        }

        // Пустой ответ
        return keys;
    }

    /**
     * Парсим ссылку на другую переменную. Значение ссылки может быть другой ссылкой, вызовом метода или выражением 'new'
     */
    @NotNull
    private ProcessVariablesKeys extractFromReference(@NotNull PsiClass containingClass, @NotNull PsiReferenceExpression referenceExpression){
        var resolvedElement = referenceExpression.resolve();
        if (resolvedElement instanceof PsiLocalVariable localVariable){
            PsiExpression localVariableValue = localVariable.getInitializer();

            boolean isList = "List<ProcessVariableDefinition>".equals(localVariable.getType().getPresentableText());
            boolean isNewInstance = "ProcessVariableDefinition".equals(localVariable.getType().getPresentableText());

            // Если значение переменной это ссылка на другую переменную, то идем к ней (рекурсивно)
            // >> List<ProcessVariableDefinition> list = anotherList;
            // >> ProcessVariableDefinition definition = anotherDefinition;
            if (localVariableValue instanceof PsiReferenceExpression anotherReferenceExpression){
                return extractFromReference(containingClass, anotherReferenceExpression);
            }

            // Если это создание списка через вызов метода (List.of, Arrays.asList), то парсим тело метода:
            // >> List<ProcessVariableDefinition> list = List.of(...)
            // Если это создание списка через 'super.defineVariables()', то собираем переменные из родителя и добавления через 'add':
            // >> List<ProcessVariableDefinition> processVariableDefinitions = super.defineVariables();
            if (isList && localVariableValue instanceof PsiMethodCallExpression methodCallExpression){
                return extractFromMethodCall(containingClass, methodCallExpression, localVariable);
            }

            // Если это создание списка через 'new', то парсим готовый список и собираем все элементы, добавленные через 'add'
            // >> List<ProcessVariableDefinition> list = new ArrayList<>(); list.add(...); list.add(...)
            if (isList && localVariableValue instanceof PsiNewExpression){
                return extractFromNewList(containingClass, localVariable);
            }

            // Если это создание нового экземпляра, то сразу извлекаем ключи из этого экземпляра
            // >> ProcessVariableDefinition def = new ProcessVariableDefinition(...)
            if (isNewInstance && localVariableValue instanceof PsiNewExpression newExpression){
                return extractKeyFromNewInstance(newExpression);
            }

            // Если это создание экземпляра с withScope, то достаем оттуда создание экземпляра и извлекаем ключи
            // >> ProcessVariableDefinition def = new ProcessVariableDefinition(...).withScope()
            if (isNewInstance && localVariableValue instanceof PsiMethodCallExpression innerMethodCall){
                if (innerMethodCall.getMethodExpression().getQualifierExpression() instanceof PsiNewExpression newExpression){
                    return extractKeyFromNewInstance(newExpression);
                }
            }
        }

        // Пустой ответ
        return new ProcessVariablesKeys();
    }

    /**
     * Извлечь ключи из вызова метода.
     * Запускает два метода в зависимости от разных условий:<ul>
     *     <li>{@link #extractFromSuperMethodCall(PsiClass, PsiLocalVariable)}</li>
     *     <li>{@link #extractFromDeclaredMethodCall(PsiClass, PsiMethodCallExpression)}</li>
     * </ul>
     */
    private ProcessVariablesKeys extractFromMethodCall(@NotNull PsiClass psiClass, @NotNull PsiMethodCallExpression methodCall, @Nullable PsiLocalVariable localVariable){
        ProcessVariablesKeys keys = new ProcessVariablesKeys();

        PsiReferenceExpression methodExpression = methodCall.getMethodExpression();
        String methodName = methodExpression.getReferenceName();
        PsiExpression qualifier = methodExpression.getQualifierExpression();
        if (qualifier == null) {
            return keys;
        }

        String qualifierAndMethod = qualifier.getText() + "." + methodName;

        // Обрабатываем вызов 'defineVariables' из родительского класса
        if ("super.defineVariables".equals(qualifierAndMethod)){
            PsiClass superClass = psiClass.getSuperClass();
            if (superClass == null) return keys;
            return extractFromSuperMethodCall(psiClass, localVariable);
        }
        // Обрабатываем только популярные конструкторы списков, которые принимают в аргументы сразу значения.
        // Вызовы любых других методов упускаем.
        else if (List.of("List.of", "Arrays.asList", "Collections.singletonList").contains(qualifierAndMethod)){
            return extractFromDeclaredMethodCall(psiClass, methodCall);
        }

        return keys;
    }

    /**
     * Извлечь ключи из метода 'defineVariables' из родителя указанного класса.
     * @param psiClass Класс, от которого берем родителя
     * @param localVariable Переменная, для которой нужно дополнительно собрать ключи из методов add (необязательная)
     * @return Ключи
     */
    private ProcessVariablesKeys extractFromSuperMethodCall(@NotNull PsiClass psiClass, @Nullable PsiLocalVariable localVariable){
        ProcessVariablesKeys keys = new ProcessVariablesKeys();

        PsiClass superClass = psiClass.getSuperClass();
        if (superClass == null) return keys;
        if (isClassNotWorker(superClass)) return keys;

        // Ищем метод List<ProcessVariableDefinition> defineVariables
        PsiMethod defineVariablesMethod = getDefineVariablesMethod(superClass, 0);
        if (defineVariablesMethod == null) return keys; // Метод не определен

        // Извлекаем все ключи из метода defineVariables
        keys = parseDefineVariables(superClass, defineVariablesMethod);
        if (localVariable != null){
            keys.merge(extractFromNewList(superClass, localVariable));
        }

        return keys;
    }

    /**
     * Извлекаем ключи из ProcessVariableDefinition внутри статических списков.
     * <ul>
     *     <li>return List.of(...)</li>
     *     <li>return Arrays.asList(...)</li>
     *     <li>return Collections.singletonList(...)</li>
     * </ul>
     */
    @NotNull
    private ProcessVariablesKeys extractFromDeclaredMethodCall(@NotNull PsiClass containingClass, @NotNull PsiMethodCallExpression methodCall){
        ProcessVariablesKeys keys = new ProcessVariablesKeys();

        for (PsiExpression argument : methodCall.getArgumentList().getExpressions()) {
            // Обработка обычных случаев (добавляется новый экземпляр ProcessVariableDefinition)
            if (argument instanceof PsiNewExpression newExpression){
                keys.merge(extractKeyFromNewInstance(newExpression));
            }
            // Если используется .withScope и другие builder-методы
            else if (argument instanceof PsiMethodCallExpression innerMethodCall){
                if (innerMethodCall.getMethodExpression().getQualifierExpression() instanceof PsiNewExpression newExpression){
                    keys.merge(extractKeyFromNewInstance(newExpression));
                }
            }
            // Если подставлена готовая переменная, хранящая ProcessVariableDefinition
            else if (argument instanceof PsiReferenceExpression referenceExpression){
                keys.merge(extractFromReference(containingClass, referenceExpression));
            }
        }

        return keys;
    }

    /**
     * Парсим выражение 'new'. Собираем все ключи, добавленные в инициализированный список через метод add.
     * Пример:
     * <pre>
     *     List<ProcessVariableDefinition> list = new ArrayList<>();
     *     list.add(new ProcessVariableDefinition(EVENT_PARAM, ..., ..., UNDEFINED, ...);
     *     list.add(new ProcessVariableDefinition("createdEvents", ..., ..., OUTPUT, ...);
     * </pre>
     */
    @NotNull
    private ProcessVariablesKeys extractFromNewList(@NotNull PsiClass containingClass, @NotNull PsiLocalVariable localVariable){
        ProcessVariablesKeys keys = new ProcessVariablesKeys();

        // Тип переменной должен быть список из ProcessVariableDefinition
        if (!"List<ProcessVariableDefinition>".equals(localVariable.getType().getPresentableText())){
            return keys;
        }

        PsiCodeBlock methodBody = PsiTreeUtil.getParentOfType(localVariable, PsiCodeBlock.class);
        if (methodBody == null) return keys;

        for (PsiStatement statement : methodBody.getStatements()) {
            if (!(statement instanceof PsiExpressionStatement expressionStatement)) continue;
            PsiExpression expression = expressionStatement.getExpression();

            if (!(expression instanceof PsiMethodCallExpression methodCall)) continue;
            PsiReferenceExpression methodExpression = methodCall.getMethodExpression();
            PsiExpression qualifier = methodExpression.getQualifierExpression();

            // Проверяем, что вызов add относится к текущей переменной
            if (qualifier != null && qualifier.getText().equals(localVariable.getName()) &&
                    List.of("add", "addAll").contains(methodExpression.getReferenceName())) {
                PsiExpression[] arguments = methodCall.getArgumentList().getExpressions();
                if (arguments.length > 0) {
                    // Если в список добавляется новый экземпляр (new ProcessVariableDefinition)
                    if (arguments[0] instanceof PsiNewExpression newExpression){
                        keys.merge(extractKeyFromNewInstance(newExpression));
                    }
                    // Если в список добавляется готовая переменная
                    if (arguments[0] instanceof PsiReferenceExpression refExpression){
                        keys.merge(extractFromReference(containingClass, refExpression));
                    }
                    // Если в список добавляется вызов метода (например addAll(List.of))
                    if (arguments[0] instanceof PsiMethodCallExpression methodCallExpression){
                        keys.merge(extractFromMethodCall(containingClass, methodCallExpression, localVariable));
                    }
                }
            }
        }

        return keys;
    }

    /**
     * Извлекаем ключи из аргументов конструктора ProcessVariableDefinition.
     */
    @NotNull
    private ProcessVariablesKeys extractKeyFromNewInstance(@NotNull PsiNewExpression newExpression) {
        ProcessVariablesKeys keys = new ProcessVariablesKeys();

        // Проверяем, что это вызов конструктора ProcessVariableDefinition
        PsiJavaCodeReferenceElement classReference = newExpression.getClassReference();
        if (classReference == null || !"ProcessVariableDefinition".equals(classReference.getReferenceName())) return keys;

        PsiExpressionList argumentList = newExpression.getArgumentList();
        if (argumentList == null) return keys;

        PsiExpression[] constructorArguments = argumentList.getExpressions();
        if (constructorArguments.length == 5) {
            String key = resolveStringValue(constructorArguments[0]);
            String direction = resolveDirectionStringFromEnum(constructorArguments[3]);

            keys.add(key, direction);
        }

        return keys;
    }

    /**
     * Извлечь значение ключа из выражения.
     */
    @Nullable
    private String resolveStringValue(@NotNull PsiExpression expression) {
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

    @Nullable
    private String resolveDirectionStringFromEnum(@NotNull PsiExpression expression){
        String expressionText = expression.getText();
        Map<String, String> fullToShort = Map.of(
                "ProcessVariableDirection.INPUT", "INPUT",
                "ProcessVariableDirection.OUTPUT", "OUTPUT",
                "ProcessVariableDirection.UNDEFINED", "UNDEFINED"
        );

        // Если enum короткий (INPUT, OUTPUT, UNDEFINED), то сразу возвращаем
        if (fullToShort.containsValue(expressionText)) return expressionText;
        // Если enum длинный, то возвращаем короткую версию или null, если не нашли
        return fullToShort.get(expressionText);
    }

}
