package me.rubix327.liquibasehelper.inspection.quickfix.base;

import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * Базовый класс для фиксов, управляющих элементами PsiElement.
 */
public abstract class BaseQuickFixOnPsiElement extends LocalQuickFixOnPsiElement {

    private final String text;

    protected BaseQuickFixOnPsiElement(@NotNull PsiElement element, @NotNull String text) {
        super(element);
        this.text = text;
    }

    public BaseQuickFixOnPsiElement(@NotNull PsiElement startElement, @NotNull PsiElement endElement, @NotNull String text) {
        super(startElement, endElement);
        this.text = text;
    }

    @Override
    public @IntentionName @NotNull String getText() {
        return text;
    }

    @Override
    public @IntentionFamilyName @NotNull String getFamilyName() {
        return text;
    }

}
