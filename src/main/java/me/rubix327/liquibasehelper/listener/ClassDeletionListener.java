package me.rubix327.liquibasehelper.listener;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import me.rubix327.liquibasehelper.StartProjectComponent;
import me.rubix327.liquibasehelper.Utils;
import me.rubix327.liquibasehelper.inspection.RulesManager;
import me.rubix327.liquibasehelper.log.MainLogger;
import me.rubix327.liquibasehelper.settings.CbsAnnotation;
import org.jetbrains.annotations.NotNull;

public class ClassDeletionListener implements PsiTreeChangeListener {

    @Override
    public void beforeChildRemoval(@NotNull PsiTreeChangeEvent psiTreeChangeEvent) {
        PsiElement removedElement = psiTreeChangeEvent.getChild();
        Project project = removedElement.getProject();
        RulesManager rulesManagerInstance = RulesManager.getInstance(project);

        // Если индексация еще не завершена, регистрируем слушателя
        if (DumbService.getInstance(project).isDumb()){
            StartProjectComponent.registerRulesForAllClassesAfterIndexingInBackground(project);
            return;
        }

        onSomethingRemoved(rulesManagerInstance, removedElement);
    }

    private void onSomethingRemoved(RulesManager rulesManagerInstance, PsiElement removedElement){
        // Удаление java-файла с классами внутри
        if (removedElement instanceof PsiJavaFile removedJavaFile) {
            for (PsiClass removedClass : removedJavaFile.getClasses()) {
                MainLogger.info(rulesManagerInstance.getProject(), "Удален класс %s.", removedClass.getQualifiedName());
                onClassRemoved(rulesManagerInstance, removedClass);
            }
        }

        // Удаление класса внутри файла
        if (removedElement instanceof PsiClass removedClass){
            MainLogger.info(rulesManagerInstance.getProject(), "Удален класс %s.", removedClass.getQualifiedName());
            onClassRemoved(rulesManagerInstance, removedClass);
        }

        // Удаление аннотации @CbsDatamodelClass
        if (removedElement instanceof PsiAnnotation removedAnnotation){
            if (removedAnnotation.getQualifiedName() == null) return;
            if (!removedAnnotation.getQualifiedName().endsWith(CbsAnnotation.CbsDatamodelClass.SHORT_NAME)) return;

            PsiClass conClass = PsiTreeUtil.getParentOfType(removedAnnotation, PsiClass.class); // Получить класс, над которым висела аннотация
            PsiField conField = PsiTreeUtil.getParentOfType(removedAnnotation, PsiField.class); // Получить поле, над которым висела аннотация

            // Проверяем, что класс существует и что это не внутренний класс (поскольку над внутренними классами не может быть этой аннотации),
            // и что аннотацию удалили не над полем
            if (conClass != null && conClass.getContainingClass() == null && conField == null){
                rulesManagerInstance.removeRulesOfClass(conClass);
                MainLogger.info(rulesManagerInstance.getProject(), "Удалена аннотация %s в классе %s.", removedAnnotation.getQualifiedName(), conClass.getName());
            }
        }
    }

    private void onClassRemoved(RulesManager rulesManager, PsiClass removedClass){
        if (Utils.isNotDatamodelClass(removedClass)) return;
        rulesManager.removeRulesOfClass(removedClass);
        rulesManager.removeClassReferencesFromEnums(removedClass);
    }

    @Override
    public void childRemoved(@NotNull PsiTreeChangeEvent psiTreeChangeEvent) {

    }

    @Override
    public void beforeChildAddition(@NotNull PsiTreeChangeEvent psiTreeChangeEvent) {

    }

    @Override
    public void beforeChildReplacement(@NotNull PsiTreeChangeEvent psiTreeChangeEvent) {

    }

    @Override
    public void beforeChildMovement(@NotNull PsiTreeChangeEvent psiTreeChangeEvent) {

    }

    @Override
    public void beforeChildrenChange(@NotNull PsiTreeChangeEvent psiTreeChangeEvent) {

    }

    @Override
    public void beforePropertyChange(@NotNull PsiTreeChangeEvent psiTreeChangeEvent) {

    }

    @Override
    public void childAdded(@NotNull PsiTreeChangeEvent psiTreeChangeEvent) {

    }

    @Override
    public void childReplaced(@NotNull PsiTreeChangeEvent psiTreeChangeEvent) {

    }

    @Override
    public void childrenChanged(@NotNull PsiTreeChangeEvent psiTreeChangeEvent) {

    }

    @Override
    public void childMoved(@NotNull PsiTreeChangeEvent psiTreeChangeEvent) {

    }

    @Override
    public void propertyChanged(@NotNull PsiTreeChangeEvent psiTreeChangeEvent) {

    }
}
