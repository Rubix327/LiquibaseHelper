package me.rubix327.liquibasehelper.inspection.quickfix;

import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import me.rubix327.liquibasehelper.Utils;
import org.jetbrains.annotations.NotNull;

public class OpenPsiElementQuickFix extends LocalQuickFixOnPsiElement {

    private final String text;

    public OpenPsiElementQuickFix(@NotNull PsiElement element, @NotNull String text) {
        super(element);
        this.text = text;
    }

    @Override
    public @IntentionFamilyName @NotNull String getFamilyName() {
        return text;
    }

    @Override
    public @IntentionName @NotNull String getText() {
        return text;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement psiElement, @NotNull PsiElement psiElement1) {
        Utils.openFile(project, psiElement);
    }

}
