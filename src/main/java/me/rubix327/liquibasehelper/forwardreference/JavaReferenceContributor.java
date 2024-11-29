package me.rubix327.liquibasehelper.forwardreference;

import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import me.rubix327.liquibasehelper.log.MainLogger;
import me.rubix327.liquibasehelper.settings.CbsAnnotation;
import org.jetbrains.annotations.NotNull;

public class JavaReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar registrar) {
        MainLogger.info("Registering Java reference providers...");

        ElementPattern<PsiLiteralExpression> enumPattern = PsiJavaPatterns.literalExpression().withParent(
                PsiJavaPatterns.psiNameValuePair().withName("availableValuesEnumPath")
                        .withSuperParent(2, PsiJavaPatterns.psiAnnotation().qName(StandardPatterns.string().matches(".*\\." + CbsAnnotation.CbsDatamodelField.SHORT_NAME)))
        );

        registrar.registerReferenceProvider(enumPattern,
                new PsiReferenceProvider() {
                    @Override
                    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
                        return new PsiReference[]{new EnumPathReferenceProvider(psiElement)};
                    }
                }
        );
    }
}