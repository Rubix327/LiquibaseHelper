package me.rubix327.liquibasehelper.docs;

import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import me.rubix327.liquibasehelper.inspection.model.AvailableValue;
import me.rubix327.liquibasehelper.settings.StaticSettings;
import me.rubix327.liquibasehelper.Utils;
import me.rubix327.liquibasehelper.inspection.RulesManager;
import me.rubix327.liquibasehelper.inspection.XmlTagValuesInspector;
import me.rubix327.liquibasehelper.inspection.model.AttributeRule;
import me.rubix327.liquibasehelper.inspection.model.TagRule;
import me.rubix327.liquibasehelper.inspection.model.TagRulesContainer;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.List;

public class TagDocumentationProvider implements DocumentationProvider {

    // Обработчик нажатий на ссылки внутри документации
    @Override
    public @Nullable PsiElement getDocumentationElementForLink(PsiManager psiManager, String link, PsiElement context) {
        if (!link.startsWith("class:") && !link.startsWith("field:")) return null;

        String[] parts = link.split(":");
        if (parts.length < 3) return null;

        String classPath = parts[1]; // class qualifiedName
        String offsetStr = parts[2]; // offset from TagRule.linkToFieldOffset or TagRulesContainer.linkToClassNameOffset

        int offset = 0;
        try{
            offset = Math.max(0, Integer.parseInt(offsetStr));
        } catch (NumberFormatException ignored){}

        PsiFile psiFile = Utils.findPsiFileByQualifiedName(context.getProject(), classPath);
        if (psiFile != null){
            Utils.openFile(context.getProject(), psiFile, offset);
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

        XmlTag xmlTag = getXmlTag(element, originalElement);
        XmlAttribute xmlAttribute = getXmlAttribute(element, originalElement);

        if (xmlTag != null){
            return getTagTooltip(xmlTag);
        }

        if (xmlAttribute != null){
            return getAttributeTooltip(xmlAttribute);
        }

        return null;
    }

    private String getAttributeTooltip(@NotNull XmlAttribute attribute) {
//        System.out.println("XML ATTRIBUTE: " + attribute + " // " + attribute.getName() + " // " + attribute.getParent() + " // " + attribute.getParent().getName());

        AttributeRule rule = AttributeRule.getSuitableAttributeRule(XmlTagValuesInspector.attributeRules, attribute);
        if (rule == null) return null;

        if (rule.attributeTooltip == null) return null;

        return rule.attributeTooltip;
    }

    private String getTagTooltip(@NotNull XmlTag tag){
        RulesManager rulesManager = RulesManager.getInstance(tag.getProject());

        TagRulesContainer tagRulesContainer = rulesManager.getRulesContainerByTagName(tag.getName());
        if (tagRulesContainer != null){
            return getParentTagTooltip(tag, tagRulesContainer);
        }

        if (tag.getParentTag() == null) return null;
        TagRulesContainer containerOfThisParent = rulesManager.getRulesContainerByTagName(tag.getParentTag().getName());
        if (containerOfThisParent == null || containerOfThisParent.getTagRules() == null || containerOfThisParent.getTagRules().isEmpty()) return null;

        TagRule rule = TagRule.getSuitableTagRule(containerOfThisParent.getTagRules(), tag);
        if (rule == null) return null;

        return getChildTagTooltip(rule);
    }

    private String getParentTagTooltip(@NotNull XmlTag tag, @NotNull TagRulesContainer tagRulesContainer){
        StringBuilder resultTooltip = new StringBuilder();

        String tooltip = tagRulesContainer.getParentTagTooltip();
        resultTooltip.append("<b>").append(Utils.isNotBlank(tooltip) ? tooltip : tag.getName()).append("</b>");
        resultTooltip.append(Utils.isNotBlank(tagRulesContainer.getParentTagDescription()) ? "<br>" + tagRulesContainer.getParentTagDescription() : "");
        List<TagRule> tagRules = tagRulesContainer.getTagRules();
        if (tagRules != null && !tagRules.isEmpty()){
            resultTooltip.append("<br><br>Возможные теги:");
            TagRule.sortByImportance(tagRules);
            for (TagRule tagRule : tagRules) {
                boolean required = tagRule.isRequired();
                resultTooltip.append("<br>• ").append(required ? "<b>" : "").append(tagRule.getTagName()).append(required ? "</b>" : "");
                if (Utils.isNotBlank(tagRule.getTagTooltip())){
                    resultTooltip
                            .append("<font color='#797D85'> - ")
                            .append(tagRule.getTagTooltip())
                            .append("</font>");
                }
            }
        }

        if (Utils.isNotBlank(tagRulesContainer.getLinkToMetaClass())){
            String link = Utils.getHtmlLink("class:" + tagRulesContainer.getLinkToMetaClass(), "Открыть мета-класс");
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
            if (rule.getMaxLength() != -1){
                resultTooltip.append("<br>Макс. длина значения: ").append(rule.getMaxLength());
            }
            resultTooltip.append(getTypeTooltip(rule));
            resultTooltip.append("<br>Обязателен: ").append(rule.isRequired() ? "Да" : "Нет");
            resultTooltip.append(getAvailableValuesTooltip(rule));
            if (Utils.isNotBlank(rule.getMetaClassPath())){
                String link = Utils.getHtmlLink("field:" + rule.getLinkToMetaField(), "Открыть поле в мета-классе");
                resultTooltip.append("<br><br>").append(link);
            }
        }

        return resultTooltip.toString();
    }

    private String getAvailableValuesTooltip(@NotNull TagRule tagRule){
        StringBuilder builder = new StringBuilder("<br><br>Возможные значения:");
        if (Boolean.class.getTypeName().equals(tagRule.getType())){
            return builder.append("<br>• 0 - Нет").append("<br>• 1 - Да").toString();
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

        StringBuilder builder = new StringBuilder("<br>Тип: ");
        if (type.equals(Long.class.getTypeName())) builder.append("Целое число");
        if (type.equals(Date.class.getTypeName())) builder.append("Дата");
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

}

