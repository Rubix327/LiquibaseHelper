package me.rubix327.liquibasehelper.docs;

import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlText;
import me.rubix327.liquibasehelper.AnnotationUtils;
import me.rubix327.liquibasehelper.Utils;
import me.rubix327.liquibasehelper.inspection.RulesManager;
import me.rubix327.liquibasehelper.inspection.XmlTagValuesInspector;
import me.rubix327.liquibasehelper.inspection.model.AttributeRule;
import me.rubix327.liquibasehelper.inspection.model.AvailableValue;
import me.rubix327.liquibasehelper.inspection.model.TagRule;
import me.rubix327.liquibasehelper.inspection.model.TagRulesContainer;
import me.rubix327.liquibasehelper.locale.Localization;
import me.rubix327.liquibasehelper.settings.StaticSettings;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

public class TagDocumentationProvider implements DocumentationProvider {

    private static final Key<String> CLASS_DOC_KEY = Key.create("classDoc");

    // Обработчик нажатий на ссылки внутри документации
    @Override
    public @Nullable PsiElement getDocumentationElementForLink(PsiManager psiManager, String link, PsiElement context) {
        // Открытие класса или поля в редакторе
        if (link.startsWith("class:") || link.startsWith("field:")){
            String[] parts = link.split(":");
            if (parts.length < 3) return null;

            String classPath = parts[1]; // class qualifiedName
            String offsetStr = parts[2]; // offset from TagRule.metaFieldOffset or TagRulesContainer.metaClassNameOffset

            int offset = 0;
            try{
                offset = Math.max(0, Integer.parseInt(offsetStr));
            } catch (NumberFormatException ignored){}

            PsiFile psiFile = Utils.findPsiFileByQualifiedName(context.getProject(), classPath);
            if (psiFile != null){
                Utils.openFile(context.getProject(), psiFile, offset);
            }
        }
        // Открытие документации другого класса изнутри существующей документации
        // Здесь в объект записывается путь к классу, для которого нужно открыть доки,
        // а затем поток передается снова в метод generateDoc
        else if (link.startsWith("classDoc:")){
            if (context instanceof XmlElement element){
                String[] parts = link.split(":");
                if (parts.length < 2) return null;
                String classPath = parts[1]; // class qualifiedName

                XmlElement copyElement = (XmlElement) element.copy();
                copyElement.putUserData(CLASS_DOC_KEY, classPath);
                return copyElement;
            }
        }

        return null;
    }

    @Override
    public @Nullable @Nls String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
//        System.out.println("element: " + element + " // " + element.getText());
//        System.out.println("originalElement: " + originalElement + " // originalElement.getText(): " + (originalElement != null ? originalElement.getText() : "null"));

        if (!StaticSettings.ENABLE_DOCUMENTATION){
            return null;
        }

        // Формирование документации другого класса из уже существующей документации
        String linkedClassPath = element.getUserData(CLASS_DOC_KEY);
        if (linkedClassPath != null){
            PsiClass linkedClass = Utils.findPsiClassByQualifiedName(element.getProject(), linkedClassPath);
            if (linkedClass != null && linkedClass.getName() != null){
                String tag = RulesManager.getDatamodelTagOfClass(linkedClass);
                assert tag != null;
                TagRulesContainer container = RulesManager.getInstance(element.getProject()).getRulesContainerByTagName(tag);
                return getParentTagTooltip(element.getProject(), tag, container);
            }
        }

        // Наведение на тег
        XmlTag xmlTag = getXmlTag(element, originalElement);
        if (xmlTag != null){
            return getTagTooltip(xmlTag);
        }

        // Наведение на атрибут
        XmlAttribute xmlAttribute = getXmlAttribute(element, originalElement);
        if (xmlAttribute != null){
            return getAttributeTooltip(xmlAttribute);
        }

        // Наведение на значение тега
        // (не сработает на значении атрибута!)
        XmlText xmlText = getXmlText(element, originalElement);
        if (xmlText != null){
            return getTextTooltip(xmlText);
        }

        return null;
    }

    private String getTextTooltip(XmlText xmlText) {
        System.out.println(xmlText.getValue());
        LocalDateTime date = Utils.getDate(xmlText.getValue());
        if (date != null){
            return DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").format(date);
        }
        return null;
    }

    private String getAttributeTooltip(@NotNull XmlAttribute attribute) {
//        System.out.println("XML ATTRIBUTE: " + attribute + " // " + attribute.getName() + " // " + attribute.getParent() + " // " + attribute.getParent().getName());

        AttributeRule rule = AttributeRule.getSuitableAttributeRule(XmlTagValuesInspector.attributeRules, attribute);
        if (rule == null) return null;

        if (rule.getAttributeTooltip() == null) return null;

        return rule.getAttributeTooltip();
    }

    private String getTagTooltip(@NotNull XmlTag tag){
        RulesManager rulesManager = RulesManager.getInstance(tag.getProject());

        TagRulesContainer tagRulesContainer = rulesManager.getRulesContainerByTagName(tag.getName());
        if (tagRulesContainer != null){
            return getParentTagTooltip(tag.getProject(), tag.getName(), tagRulesContainer);
        }

        if (tag.getParentTag() == null) return null;
        TagRulesContainer containerOfThisParent = rulesManager.getRulesContainerByTagName(tag.getParentTag().getName());
        if (containerOfThisParent == null || containerOfThisParent.getTagRules() == null || containerOfThisParent.getTagRules().isEmpty()) return null;

        TagRule rule = TagRule.getSuitableTagRule(containerOfThisParent.getTagRules(), tag);
        if (rule == null) return null;

        return getChildTagTooltip(rule);
    }

    private String getParentTagTooltip(@NotNull Project project, @NotNull String fallbackName, @NotNull TagRulesContainer tagRulesContainer){
        StringBuilder resultTooltip = new StringBuilder();

        String tooltip = tagRulesContainer.getParentTagTooltip();
        resultTooltip.append("<b>").append(Utils.isNotBlank(tooltip) ? tooltip : fallbackName).append("</b>");
        resultTooltip.append(Utils.isNotBlank(tagRulesContainer.getParentTagDescription()) ? "<br>" + tagRulesContainer.getParentTagDescription() : "");
        List<TagRule> tagRules = tagRulesContainer.getTagRules();
        if (tagRules != null && !tagRules.isEmpty()){
            resultTooltip.append("<br><br>").append(Localization.message("docs.available-tags"));
            TagRule.sortByImportance(tagRules);
            for (TagRule tagRule : tagRules) {
                boolean required = tagRule.isRequired();
                resultTooltip.append("<br>• ").append(required ? "<i>" : "");

                // Формирование ссылки на класс поля со списочным типом, если он является @CbsDatamodelClass
                // Например List<EnumerationValueMeta> - формируем ссылку на класс EnumerationValueMeta
                PsiClass linkedClass = Utils.findPsiClassByQualifiedName(project, tagRule.getListLinkToBaseClass());
                if (linkedClass != null && linkedClass.getQualifiedName() != null && !AnnotationUtils.isNotDatamodelClass(linkedClass)){
                    String link = Utils.getHtmlLink("classDoc:" + linkedClass.getQualifiedName(), tagRule.getTagName());
                    resultTooltip.append(link);
                }
                // Обычный случай (просто название тега из правила)
                else {
                    resultTooltip.append(tagRule.getTagName());
                }

                resultTooltip.append(required ? "</i>" : "");
                if (Utils.isNotBlank(tagRule.getTagTooltip())){
                    resultTooltip
                            .append("<font color='#797D85'> - ")
                            .append(tagRule.getTagTooltip())
                            .append("</font>");
                }
            }
        }

        if (Utils.isNotBlank(tagRulesContainer.getLinkToMetaClassWithOffset())){
            String link = Utils.getHtmlLink("class:" + tagRulesContainer.getLinkToMetaClassWithOffset(), Localization.message("docs.open-meta-class"));
            resultTooltip.append("<br><br>").append(link);
        }

        return resultTooltip.toString();
    }

    private String getChildTagTooltip(@NotNull TagRule rule){
        StringBuilder resultTooltip = new StringBuilder();

        resultTooltip.append("<b>").append(Utils.isNotBlank(rule.getTagTooltip()) ? rule.getTagTooltip() : rule.getTagName()).append("</b>");
        resultTooltip.append(Utils.isNotBlank(rule.getTagDescription()) ? "<br>" + rule.getTagDescription() : "");
        if (rule.isExtendedTooltipInfo()){
            resultTooltip.append("<br>");
            if (rule.getMaxLength() > 0){
                resultTooltip.append("<br>").append(Localization.message("docs.child-tag.max-length")).append(" ").append(rule.getMaxLength());
            }
            resultTooltip.append(getTypeTooltip(rule));
            resultTooltip.append("<br>").append(Localization.message("docs.child-tag.required")).append(" ")
                    .append(Localization.message(rule.isRequired() ? "docs.yes" : "docs.no"));
            resultTooltip.append(getAvailableValuesTooltip(rule));
            if (Utils.isNotBlank(rule.getMetaClassPath())){
                String link = Utils.getHtmlLink("field:" + rule.getLinkToMetaFieldWithOffset(), Localization.message("docs.open-meta-field"));
                resultTooltip.append("<br><br>").append(link);
            }
        }

        return resultTooltip.toString();
    }

    private String getAvailableValuesTooltip(@NotNull TagRule tagRule){
        StringBuilder builder = new StringBuilder("<br><br>").append(Localization.message("docs.child-tag.available-values"));
        if (Boolean.class.getTypeName().equals(tagRule.getType())){
            return builder
                    .append("<br>").append(Localization.message("docs.child-tag.required.no"))
                    .append("<br>").append(Localization.message("docs.child-tag.required.yes"))
                    .toString();
        } else if (Utils.isNotEmpty(tagRule.getAvailableValues())) {
            for (AvailableValue availableValue : tagRule.getAvailableValues()) {
                builder.append("<br>• ").append(availableValue.getValue());
                if (availableValue.getComment() != null){
                    builder.append(" - ").append(availableValue.getComment());
                }
            }
            return builder.toString();
        } else {
            return "";
        }
    }

    private String getTypeTooltip(@NotNull TagRule tagRule){
        String type = tagRule.getType();
        if (type == null) return "";
        if (type.equals(String.class.getTypeName())) return "";
        if (type.equals(Boolean.class.getTypeName())) return "";

        StringBuilder builder = new StringBuilder("<br>").append(Localization.message("docs.child-tag.type")).append(" ");
        if (type.equals(Long.class.getTypeName())) builder.append(Localization.message("docs.child-tag.type.integer"));
        if (type.equals(Double.class.getTypeName())) builder.append(Localization.message("docs.child-tag.type.double"));
        if (type.equals(Date.class.getTypeName())) builder.append(Localization.message("docs.child-tag.type.date"));
        return builder.toString();
    }

    private XmlAttribute getXmlAttribute(PsiElement element, @Nullable PsiElement originalElement){
        if (element instanceof XmlAttribute) {
            return (XmlAttribute) element;
        } else if (originalElement instanceof XmlAttribute) {
            return (XmlAttribute) originalElement;
        } else if (originalElement != null && originalElement.getParent() instanceof XmlAttribute) {
            return (XmlAttribute) originalElement.getParent();
        }
        return null;
    }

    private XmlTag getXmlTag(PsiElement element, @Nullable PsiElement originalElement) {
        if (element instanceof XmlTag) {
            return (XmlTag) element;
        } else if (originalElement instanceof XmlTag) {
            return (XmlTag) originalElement;
        } else if (originalElement != null && originalElement.getParent() instanceof XmlTag) {
            return (XmlTag) originalElement.getParent();
        }
        return null;
    }

    private XmlText getXmlText(PsiElement element, @Nullable PsiElement originalElement){
        if (element instanceof XmlText) {
            return (XmlText) element;
        } else if (originalElement instanceof XmlText) {
            return (XmlText) originalElement;
        } else if (originalElement != null && originalElement.getParent() instanceof XmlText) {
            return (XmlText) originalElement.getParent();
        }
        return null;
    }

}

