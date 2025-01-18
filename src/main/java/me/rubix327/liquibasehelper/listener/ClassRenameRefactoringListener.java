package me.rubix327.liquibasehelper.listener;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.refactoring.listeners.RefactoringElementListenerProvider;
import me.rubix327.liquibasehelper.inspection.RulesManager;
import me.rubix327.liquibasehelper.log.MainLogger;
import org.jetbrains.annotations.NotNull;

public class ClassRenameRefactoringListener implements RefactoringElementListenerProvider {

    @Override
    public RefactoringElementListener getListener(@NotNull PsiElement element) {
        if (element instanceof PsiClass oldClass) {
            String oldQualifiedName = oldClass.getQualifiedName();
            String oldDatamodelName = RulesManager.getDatamodelTagOfClass(oldClass);

            return new RefactoringElementListener() {
                @Override
                public void elementMoved(@NotNull PsiElement psiElement) {
                    onElementMovedOrRenamed(psiElement);
                }

                @Override
                public void elementRenamed(@NotNull PsiElement psiElement) {
                    onElementMovedOrRenamed(psiElement);
                }

                void onElementMovedOrRenamed(@NotNull PsiElement psiElement){
                    if (!(psiElement instanceof PsiClass newClass)) return;

                    MainLogger.info(element.getProject(), "Class has been renamed from %s to %s", oldQualifiedName, newClass.getQualifiedName());

                    RulesManager rulesManagerInstance = RulesManager.getInstance(newClass.getProject());
                    String newDatamodelName = RulesManager.getDatamodelTagOfClass(newClass);

                    // Обновляем регистр qualifiedName -> datamodelName
                    rulesManagerInstance.removeDatamodelValueFromRegistry(oldQualifiedName);
                    if (newDatamodelName != null && newClass.getQualifiedName() != null){
                        rulesManagerInstance.putDatamodelValueToRegistry(newClass.getQualifiedName(), newDatamodelName);
                    }

                    // Обновляем регистр правил, привязанных к datamodelName
                    rulesManagerInstance.removeRulesByTagNameAndClass(oldQualifiedName, oldDatamodelName);
                    rulesManagerInstance.handleClassAndSuperClasses(newClass, "ClassRenameRefactoringListener: newClass"); // TODO что будет при рефакторинге во время индексации?

                    // Ссылки из енумов на изменившийся класс - обновляются автоматически
                }

            };
        }
        return null;
    }
}

