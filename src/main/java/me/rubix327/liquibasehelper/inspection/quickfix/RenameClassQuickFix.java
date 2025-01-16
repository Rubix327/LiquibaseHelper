package me.rubix327.liquibasehelper.inspection.quickfix;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.rename.RenameProcessor;
import me.rubix327.liquibasehelper.inspection.quickfix.base.BaseQuickFixOnPsiElement;
import me.rubix327.liquibasehelper.locale.Localization;
import org.jetbrains.annotations.NotNull;

public class RenameClassQuickFix extends BaseQuickFixOnPsiElement {

    private final String newClassName;

    public RenameClassQuickFix(@NotNull PsiClass element, String newClassName) {
        super(element, getDefaultText(newClassName));
        this.newClassName = newClassName;
    }

    public RenameClassQuickFix(@NotNull PsiClass psiClass, @NotNull String text, @NotNull String newClassName) {
        super(psiClass, text);
        this.newClassName = newClassName;
    }

    private static String getDefaultText(String newClassName){
        return Localization.message("class.quickfix.rename", newClassName);
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement psiElement, @NotNull PsiElement psiElement1) {
        if (!(psiElement instanceof PsiClass psiClass)) return;

        CommandProcessor.getInstance().executeCommand(
                project,
                () -> ApplicationManager.getApplication().invokeLater(() -> {
                    if (psiClass.isValid()) {
                        RenameProcessor renameProcessor = new RenameProcessor(
                                project, psiClass, newClassName, false, false
                        );
                        renameProcessor.run();
                    }
                }), "Rename Class", null
        );
    }

}
