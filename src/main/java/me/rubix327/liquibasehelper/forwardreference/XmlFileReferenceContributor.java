package me.rubix327.liquibasehelper.forwardreference;

import com.intellij.patterns.StandardPatterns;
import com.intellij.patterns.XmlAttributeValuePattern;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import me.rubix327.liquibasehelper.log.MainLogger;

public class XmlFileReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar registrar) {
        MainLogger.info("Registering XML reference providers...");

        // Регистрируем обработчик ссылок на атрибуты "file" в XML
        XmlAttributeValuePattern pattern = XmlPatterns.xmlAttributeValue().withLocalName("file");
        registrar.registerReferenceProvider(
                pattern,
                new XmlFileReferenceProvider(false)
        );

        // Добавляем поддержку value атрибута, когда name="fileName"
        registrar.registerReferenceProvider(
                XmlPatterns.xmlAttributeValue().withLocalName("value")
                        .withSuperParent(2, XmlPatterns.xmlTag()
                                .withName("param")
                                .withChild(XmlPatterns.xmlAttribute()
                                        .withName("name")
                                        .withValue(StandardPatterns.string().equalTo("fileName"))
                                )
                        ),
                new XmlFileReferenceProvider(true)
        );

        // Поддержка открытия методов, классов и пакетов внутри actionCode с actionTypeCode=ExecuteService
        registrar.registerReferenceProvider(
                XmlPatterns.xmlTag().withLocalName("actionCode")
                        .withParent(XmlPatterns.xmlTag()
                                        .withLocalName("entityOperationMeta")
                                        .withChild(XmlPatterns.xmlTag()
                                                .withLocalName("actionTypeCode")
                                                .withText("<actionTypeCode>ExecuteService</actionTypeCode>")
                                        )
                        ),
                new JavaPathReferenceProvider()
        );

    }
}
