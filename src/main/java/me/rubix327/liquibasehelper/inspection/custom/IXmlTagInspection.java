package me.rubix327.liquibasehelper.inspection.custom;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

public interface IXmlTagInspection {

    void checkForTagConstraints(@NotNull XmlTag tag, @NotNull ProblemsHolder holder);

}
