package me.rubix327.liquibasehelper.inspection.quickfix;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import me.rubix327.liquibasehelper.inspection.quickfix.base.BaseQuickFixOnPsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * Фикс для установки аннотации на любой элемент, поддерживающий аннотации.
 */
public class AddAnnotationQuickFix extends BaseQuickFixOnPsiElement {

    private final String annotationName;

    public AddAnnotationQuickFix(@NotNull PsiModifierListOwner element, @NotNull String text, @NotNull String annotationName) {
        super(element, text);
        this.annotationName = annotationName;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement psiElement, @NotNull PsiElement psiElement1) {
        if (!(psiElement instanceof PsiModifierListOwner element)) return;

        PsiModifierList modifierList = element.getModifierList();
        if (modifierList == null) {
            return; // Если у элемента нет модификаторов
        }
        if (modifierList.findAnnotation(annotationName) != null){
            return; // Если аннотация уже существует
        }

        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        PsiAnnotation annotation = factory.createAnnotationFromText("@" + annotationName, element);

        // Добавить аннотацию
        modifierList.addBefore(annotation, modifierList.getFirstChild());
    }

}
