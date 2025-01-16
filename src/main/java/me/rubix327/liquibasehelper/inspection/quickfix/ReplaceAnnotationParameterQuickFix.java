package me.rubix327.liquibasehelper.inspection.quickfix;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import me.rubix327.liquibasehelper.inspection.quickfix.base.BaseQuickFixOnPsiElement;
import me.rubix327.liquibasehelper.locale.Localization;
import org.jetbrains.annotations.NotNull;

public class ReplaceAnnotationParameterQuickFix extends BaseQuickFixOnPsiElement {

    private final String newValue;

    public ReplaceAnnotationParameterQuickFix(@NotNull PsiNameValuePair pair, @NotNull String newValue) {
        super(pair, Localization.message("field.quickfix.replace-type-param", newValue));
        this.newValue = newValue;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement psiElement, @NotNull PsiElement psiElement1) {
        if (!(psiElement instanceof PsiNameValuePair pair)) {
            return;
        }

        // Создаём новое значение для параметра
        PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
        String s = String.format("@Annotation(%s=%s)", pair.getAttributeName(), newValue);
        PsiNameValuePair newPair = factory.createAnnotationFromText(s, pair.getContext()).getParameterList().getAttributes()[0];

        // Заменяем старое значение на новое
        pair.replace(newPair);
    }
}
