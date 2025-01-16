package me.rubix327.liquibasehelper.inspection.quickfix;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import me.rubix327.liquibasehelper.Utils;
import me.rubix327.liquibasehelper.inspection.quickfix.base.BaseQuickFixOnPsiElement;
import me.rubix327.liquibasehelper.locale.Localization;
import org.jetbrains.annotations.NotNull;

/**
 * Фикс для удаления элементов PsiElement из кода.
 */
public class RemovePsiElementQuickFix extends BaseQuickFixOnPsiElement {

    public RemovePsiElementQuickFix(@NotNull PsiElement element, @NotNull String text) {
        super(element, text);
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement psiElement, @NotNull PsiElement psiElement1) {
        if (psiElement.isValid()){
            psiElement.delete();
        }

        Utils.optimizeImports(project, psiFile);
    }

    public static RemovePsiElementQuickFix getRemovePsiElementFix(PsiNameValuePair nameValuePair){
        return new RemovePsiElementQuickFix(nameValuePair, Localization.message("field.quickfix.delete-param", nameValuePair.getAttributeName()));
    }

}
