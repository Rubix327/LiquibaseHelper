package me.rubix327.liquibasehelper.inspection.quickfix;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import me.rubix327.liquibasehelper.Utils;
import me.rubix327.liquibasehelper.inspection.quickfix.base.BaseQuickFixOnPsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * Фикс для открытия элементов в редакторе.
 */
public class OpenPsiElementQuickFix extends BaseQuickFixOnPsiElement {

    public OpenPsiElementQuickFix(@NotNull PsiElement element, @NotNull String text) {
        super(element, text);
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement psiElement, @NotNull PsiElement psiElement1) {
        Utils.openFile(project, psiElement);
    }

}
