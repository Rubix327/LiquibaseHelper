package me.rubix327.liquibasehelper.forwardreference;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.search.GlobalSearchScope;

class EnumPathReferenceProvider extends PsiReferenceBase<PsiElement> {

    public EnumPathReferenceProvider(PsiElement element) {
        super(element, new TextRange(1, element.getTextLength() - 1)); // Убираем кавычки из текста
    }

    @Override
    public PsiElement resolve() {
        Project project = getElement().getProject();
        String qualifiedName = getElement().getText().replace("\"", "");

        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);

        // Ищем класс
        PsiClass psiClass = javaPsiFacade.findClass(qualifiedName, scope);
        if (psiClass != null){
            return psiClass;
        }

        // Если класс не нашли, то ищем пакет
        return javaPsiFacade.findPackage(qualifiedName);
    }

}
