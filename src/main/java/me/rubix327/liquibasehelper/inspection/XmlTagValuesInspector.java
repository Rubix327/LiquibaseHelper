package me.rubix327.liquibasehelper.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.XmlElementVisitor;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import me.rubix327.liquibasehelper.Utils;
import me.rubix327.liquibasehelper.inspection.model.*;
import me.rubix327.liquibasehelper.locale.DeclinationHelper;
import me.rubix327.liquibasehelper.locale.Localization;
import me.rubix327.liquibasehelper.settings.StaticSettings;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class XmlTagValuesInspector extends LocalInspectionTool {

    public static final List<AttributeRule> attributeRules = new ArrayList<>(){{
        add(new AttributeRule("id").setMustParentName("changeSet").setMaxLength(255).setAttributeTooltip("Идентификатор ченджсета. Должен быть уникальным в разрезе этого файла."));
        add(new AttributeRule("author").setMustParentName("changeSet").setMaxLength(255).setAttributeTooltip("Автор этого ченджсета."));
        add(new AttributeRule("constraintName").setMustGrandParentName("changeSet").setMaxLength(30).setAttributeTooltip("Название ограничения"));
        add(new AttributeRule("columnName").setMustGrandParentName("changeSet").setMaxLength(30).setAttributeTooltip("Название колонки"));
        add(new AttributeRule("tableName").setMustGrandParentName("changeSet").setMaxLength(30).setAttributeTooltip("Название таблицы"));
        add(new AttributeRule("tablespace").setMustGrandParentName("changeSet").setMaxLength(30));
        add(new AttributeRule("baseTableName").setMustGrandParentName("changeSet").setMaxLength(30));
        add(new AttributeRule("indexName").setMustGrandParentName("changeSet").setMaxLength(30).setAttributeTooltip("Название индекса"));
        add(new AttributeRule("oldTableName").setMustGrandParentName("changeSet").setMaxLength(30).setAttributeTooltip("Старое название таблицы"));
        add(new AttributeRule("newTableName").setMustGrandParentName("changeSet").setMaxLength(30).setAttributeTooltip("Новое название таблицы"));
        add(new AttributeRule("oldColumnName").setMustGrandParentName("changeSet").setMaxLength(30).setAttributeTooltip("Старое название колонки"));
        add(new AttributeRule("newColumnName").setMustGrandParentName("changeSet").setMaxLength(30).setAttributeTooltip("Новое название колонки"));
        add(new AttributeRule("name").setMustParentName("column").setMaxLength(30).setAttributeTooltip("Название колонки"));
    }};

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new XmlElementVisitor() {

            @Override
            public void visitXmlFile(@NotNull XmlFile file) {
                if (!StaticSettings.ENABLE_INSPECTIONS){
                    return;
                }

                if (file.getVirtualFile() != null && file.getVirtualFile().getPath().contains("/datamodel/")) {
                    super.visitFile(file);
                }
            }

            @Override
            public void visitXmlTag(@NotNull XmlTag tag) {
                if (!StaticSettings.ENABLE_INSPECTIONS){
                    return;
                }

                super.visitXmlTag(tag);

                checkForTagConstraints(tag, holder);
                checkForRequiredTags(tag, holder);
            }

            @Override
            public void visitXmlAttribute(@NotNull XmlAttribute attribute) {
                if (!StaticSettings.ENABLE_INSPECTIONS){
                    return;
                }

                super.visitXmlAttribute(attribute);

                checkForAttributeConstraints(attribute, holder);
            }
        };
    }

    private void checkForAttributeConstraints(@NotNull XmlAttribute attribute, @NotNull ProblemsHolder holder){
        if (attribute.getParent() == null || attribute.getParent().getParentTag() == null) return;
        XmlTag parent = attribute.getParent();
        if (parent == null) return;

        AttributeRule rule = AttributeRule.getSuitableAttributeRule(attributeRules, attribute);
        if (rule == null) return;

        String attributeText = attribute.getValue();
        if (Utils.isNotEmpty(rule.getAvailableValues()) && !rule.getAvailableValues().contains(attributeText)){
            Utils.registerError(holder, attribute, Localization.message("attribute.warn.available-values", rule.getAvailableValues()));
        }

        if (attributeText == null) return;
        if (rule.getMaxLength() > 0 && attributeText.length() > rule.getMaxLength()){
            Utils.registerError(holder, attribute, DeclinationHelper.CHARACTER_NOMINATIVE_ATTR.message(rule.getMaxLength()));
        }
    }

    public static boolean isTagSuitableByNeighbours(@NotNull XmlTag tag, @NotNull List<AttributeNeighbour> neighbours){
        for (AttributeNeighbour neighbour : neighbours) {
            if (neighbour.attributeName == null) continue; // Если не заполнено название соседа, то подходит

            XmlAttribute attribute = tag.getAttribute(neighbour.attributeName);
            if (attribute == null) return false; // Если у соседа есть название, но его нет в теге, то не подходит
            if (neighbour.attributeValue == null) continue; // Если значение не заполнено, то подходит

            if (attribute.getValue() == null) return false;
        }
        return true;
    }

    /**
     * Проверки на ограничения тегов.
     */
    private void checkForTagConstraints(@NotNull XmlTag tag, @NotNull ProblemsHolder holder){
        if (tag.getParentTag() == null) return;

        TagRulesContainer containerOfThisParent = RulesManager.getInstance(tag.getProject()).getRulesContainerByTagName(tag.getParentTag().getName());
        if (containerOfThisParent == null || Utils.isEmpty(containerOfThisParent.getTagRules())) return;

        TagRule rule = TagRule.getSuitableTagRule(containerOfThisParent.getTagRules(), tag);
        if (rule == null) return;

        String tagText = tag.getValue().getText();

        // Проверка на максимальную длину
        if (rule.getMaxLength() > 0 && tagText.length() > rule.getMaxLength()){
            Utils.registerErrorOnValueOrTag(holder, tag, DeclinationHelper.CHARACTER_NOMINATIVE_TAG.message(rule.getMaxLength()));
        }

        // Проверка на обязательность
        if (tagText.isEmpty() && rule.isRequired()){
            Utils.registerErrorOnValueOrTag(holder, tag, Localization.message("tag.warn.required"));
            return;
        }

        if (tagText.isEmpty()) return;

        // Проверка на целое число
        if (Long.class.getTypeName().equals(rule.getType())){
            try{
                Long.parseLong(tagText);
            } catch (NumberFormatException e){
                Utils.registerErrorOnValueOrTag(holder, tag, Localization.message("tag.warn.must-be-integer"));
            }
        }
        // Проверка на число с плавающей точкой
        else if (Double.class.getTypeName().equals(rule.getType())){
            try{
                Double.parseDouble(tagText);
            } catch (NumberFormatException e){
                Utils.registerErrorOnValueOrTag(holder, tag, Localization.message("tag.warn.must-be-double"));
            }
        }
        // Проверка на 0, 1
        else if (Boolean.class.getTypeName().equals(rule.getType())){
            if (!List.of("0", "1").contains(tagText)){
                Utils.registerErrorOnValueOrTag(holder, tag, Localization.message("tag.warn.must-be-boolean"));
            }
        }
        // Проверка на дату
        else if (Date.class.getTypeName().equals(rule.getType())){
            if (!Utils.isDate(tagText)){
                Utils.registerErrorOnValueOrTag(holder, tag, Localization.message("tag.warn.must-be-date"));
            }
        }

        // Проверка на допустимые значения
        // Не заполняются при type=Boolean
        List<String> availableValuesStrings = rule.getAvailableValues().stream().map(AvailableValue::getValue).toList();
        if (Utils.isNotEmpty(rule.getAvailableValues()) && !availableValuesStrings.contains(tagText)){
            Utils.registerErrorOnValueOrTag(holder, tag, Localization.message("tag.warn.must-be-following", availableValuesStrings));
        }

    }

    /**
     * Проверка на обязательность тегов внутри родительских.
     * @param parentTag Родительский тег
     */
    private void checkForRequiredTags(@NotNull XmlTag parentTag, @NotNull ProblemsHolder holder){
        if (parentTag.getParentTag() == null) return;

        TagRulesContainer containerOfThisParent = RulesManager.getInstance(parentTag.getProject()).getRulesContainerByTagName(parentTag.getName());
        if (containerOfThisParent == null || containerOfThisParent.getTagRules() == null || containerOfThisParent.getTagRules().isEmpty()) return;

        List<String> requiredTags = new ArrayList<>(containerOfThisParent.getTagRules().stream().filter(TagRule::isRequired).map(TagRule::getTagName).toList());

        List<String> childrenTags = Utils.getXmlTagsFromPsiElements(parentTag.getChildren()).stream().map(XmlTag::getName).toList();
        requiredTags.removeAll(childrenTags);

        if (!requiredTags.isEmpty()){
            Utils.registerErrorOnElement(holder, parentTag, Localization.message("tag.warn.must-include-required-tags", requiredTags));
        }
    }

}

