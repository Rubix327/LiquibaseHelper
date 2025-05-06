package me.rubix327.liquibasehelper.forwardreference.base;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JavaClassOrPackageReference extends PsiReferenceBase<XmlTag> {

    private final String path;

    public JavaClassOrPackageReference(@NotNull XmlTag element, @NotNull TextRange range, @NotNull String path) {
        super(element, range);
        this.path = path;
    }

    @Override
    public @Nullable PsiElement resolve() {
        Project project = getElement().getProject();
        JavaPsiFacade facade = JavaPsiFacade.getInstance(project);

        PsiClass psiClass = facade.findClass(path, GlobalSearchScope.allScope(project));
        if (psiClass != null) return psiClass;

        return facade.findPackage(path);
    }
}
