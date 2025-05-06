package me.rubix327.liquibasehelper.forwardreference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ProcessingContext;
import me.rubix327.liquibasehelper.forwardreference.base.JavaClassOrPackageReference;
import me.rubix327.liquibasehelper.forwardreference.base.JavaMethodReference;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JavaPathReferenceProvider extends PsiReferenceProvider {

    @Override
    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                           @NotNull ProcessingContext context) {
        if (!(element instanceof XmlTag xmlTag)) return PsiReference.EMPTY_ARRAY;

        String fullPath = xmlTag.getValue().getText(); // "org.example.SomeClass.myMethod"
        int methodDotIndex = fullPath.lastIndexOf('.');
        if (methodDotIndex == -1) return PsiReference.EMPTY_ARRAY;

        List<PsiReference> references = getReferences(xmlTag);
        PsiReference[] array = new PsiReference[references.size()];
        references.toArray(array);
        return array;
    }

    private List<PsiReference> getReferences(XmlTag xmlTag){
        String fullPath = xmlTag.getValue().getText(); // "org.example.SomeClass.myMethod"
        String[] parts = fullPath.split("\\.");
        int offset = xmlTag.getText().indexOf(fullPath);
        List<PsiReference> references = new ArrayList<>();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            TextRange range = TextRange.from(offset, part.length());

            PsiReference ref;
            if (i == parts.length - 1) {
                // Последний элемент — это метод или класс или пакет
                ref = new JavaMethodReference(xmlTag, range, fullPath);
                if (ref.resolve() == null){
                    String classOrPackagePath = String.join(".", Arrays.copyOfRange(parts, 0, i + 1));
                    ref = new JavaClassOrPackageReference(xmlTag, range, classOrPackagePath);
                }
            } else {
                // Остальные — пакеты и класс
                String classOrPackagePath = String.join(".", Arrays.copyOfRange(parts, 0, i + 1));
                ref = new JavaClassOrPackageReference(xmlTag, range, classOrPackagePath);
            }

            references.add(ref);
            offset += part.length() + 1; // +1 за точку
        }
        return references;
    }

}
