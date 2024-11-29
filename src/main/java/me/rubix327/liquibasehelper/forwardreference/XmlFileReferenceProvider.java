package me.rubix327.liquibasehelper.forwardreference;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.util.ProcessingContext;
import me.rubix327.liquibasehelper.settings.StaticSettings;
import org.jetbrains.annotations.NotNull;

public class XmlFileReferenceProvider extends PsiReferenceProvider {

    private final boolean isMetadata;

    public XmlFileReferenceProvider(boolean isMetadata) {
        this.isMetadata = isMetadata;
    }

    @Override
    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                           @NotNull ProcessingContext context) {
        if (!StaticSettings.ENABLE_REFERENCES){
            return PsiReference.EMPTY_ARRAY;
        }

        if (!(element instanceof XmlAttributeValue xmlAttributeValue)) {
            return PsiReference.EMPTY_ARRAY;
        }

        String filePath = xmlAttributeValue.getValue();
        // Создаем и возвращаем PsiReference для ссылки на файл
        return new PsiReference[]{new XmlFileReference(xmlAttributeValue, filePath, isMetadata)};
    }
}
