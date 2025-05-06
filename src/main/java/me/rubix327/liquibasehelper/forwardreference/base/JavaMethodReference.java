package me.rubix327.liquibasehelper.forwardreference.base;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.xml.XmlTag;
import me.rubix327.liquibasehelper.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JavaMethodReference extends PsiReferenceBase<XmlTag> {

    private final String path;

    public JavaMethodReference(@NotNull XmlTag element, @NotNull TextRange rangeInElement, @NotNull String path) {
        super(element, rangeInElement);
        this.path = path;
    }

    @Override
    public @Nullable PsiElement resolve() {
        int methodDotIndex = path.lastIndexOf('.');
        if (methodDotIndex == -1) return null;

        String beanName = path.substring(0, methodDotIndex);
        String methodName = path.substring(methodDotIndex + 1);

        PsiClass psiClass = Utils.findPsiClassByQualifiedName(myElement.getProject(), beanName);
        if (psiClass == null) return null;

        for (PsiMethod method : psiClass.getMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }

        return null;
    }

}
