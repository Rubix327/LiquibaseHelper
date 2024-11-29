package me.rubix327.liquibasehelper.forwardreference;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.xml.XmlAttributeValue;
import me.rubix327.liquibasehelper.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class XmlFileReference extends PsiReferenceBase<XmlAttributeValue> {
    private final String filePath;
    private final boolean isMetadata;

    public XmlFileReference(@NotNull XmlAttributeValue element, String filePath, boolean isMetadata) {
        super(element);
        this.filePath = filePath;
        this.isMetadata = isMetadata;
    }

    @Override
    public @Nullable PsiElement resolve() {
        VirtualFile virtualFile;
        if (isMetadata){
            virtualFile = Utils.findFileInProject(getElement().getProject(), filePath, "/datamodel/resources/", "/metadata/");
        } else {
            String actualFilePath = "/datamodel/resources/" + filePath;
            virtualFile = getElement().getProject().getBaseDir().findFileByRelativePath(actualFilePath);
        }

        if (virtualFile != null) {
            return PsiManager.getInstance(getElement().getProject()).findFile(virtualFile);
        }

        return null;
    }

}

