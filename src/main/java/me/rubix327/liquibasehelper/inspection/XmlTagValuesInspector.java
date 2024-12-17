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

//    public static final Map<String, List<TagRule>> tagRules = new HashMap<>(){{
//        put("accIntentionTreeMeta", List.of(
//                new StringTagRule("accLabel", 255),
//                new StringTagRule("parentCode", 50),
//                new StringTagRule("externalCode", 50),
//                new StringTagRule("label", 50),
//                new StringTagRule("code", 50),
//                new StringTagRule("description", 255),
//                new StringTagRule("bankOperationTypeCode", 255),
//                new StringTagRule("digitalCode", 50)
//        ));
//        put("accountTypeWrapperMetas", List.of(
//                new StringTagRule("code", 50),
//                new StringTagRule("parentCode", 50),
//                new StringTagRule("description", 255),
//                new StringTagRule("externalCode", 50),
//                new TagRule("isGroup", Integer.class.getTypeName()),
//                new TagRule("level", Integer.class.getTypeName()),
//                new StringTagRule("label", 255),
//                new StringTagRule("labelEn", 50),
//                new StringTagRule("isActive", 255, List.of("Active", "Passive", "Any"))
//        ));
//        put("accIntentionGenAccWrapperMeta", List.of(
//                new StringTagRule("label", 100),
//                new StringTagRule("accIntentionCode", 50),
//                new StringTagRule("accountTypeCode", 50)
//        ));
//        put("modelFullMeta", List.of(
//                new StringTagRule("code", 50),
//                new StringTagRule("label", 50),
//                new StringTagRule("description", 255),
//                new StringTagRule("modelEntityClassCode", 100)
//        ));
//        put("modelAccIntentionMeta", List.of(
//                new StringTagRule("label", 50),
//                new StringTagRule("modelCode", 50),
//                new StringTagRule("accIntentionCode", 50),
//                new StringTagRule("accDetermineOptionCode", 255, List.of("Document", "AccountingModel", "AccountGroup")),
//                new StringTagRule("accOpenOptionCode", 255, List.of("Manual", "EventBased")),
//                new StringTagRule("accCurrencyOptionCode", 255, List.of("CurrencyGroup", "DocumentCurrency", "CurrencyAfterConversion", "AccountCurrency", "NoCurrency", "EventCurrency", "CurrencyContext")),
//                new StringTagRule("maybeSetInDocCode", -1, List.of("0", "1")),
//                new StringTagRule("maybeSetInPact", -1, List.of("0", "1"))
//        ));
//        put("modelAccIntentionAccountMeta", List.of(
//                new StringTagRule("label", 50),
//                new StringTagRule("modelCode", 50),
//                new StringTagRule("accIntentionCode", 50)
//        ));
//        put("modelEventMeta", List.of(
//                new StringTagRule("code", 50),
//                new StringTagRule("description", 255),
//                new StringTagRule("modelCode", 50),
//                new TagRule("directionCode", Integer.class.getTypeName(), List.of("1", "2", "3"), -1),
//                new TagRule("typeCode", Integer.class.getTypeName(), 1)
//        ));
//        put("modelEventAccountMeta", List.of(
//                new StringTagRule("label", 50),
//                new StringTagRule("modelCode", 50),
//                new StringTagRule("modelEventCode", 50),
//                new StringTagRule("accIntentionCode", 50),
//                new StringTagRule("isForceOpenCode", -1, List.of("0", "1")),
//                new StringTagRule("accountLabel", 255)
//        ));
//        put("corrAccountEventMeta", List.of(
//                new StringTagRule("modelCode", 50),
//                new StringTagRule("modelEventCode", 50),
//                new StringTagRule("payDocTypeCode", 255, List.of("INTERNAL_TRANSFER",
//                        "INTERNAL_ACCOUNT_TRANSFER", "NOT_BALANCE_TRANSFER", "CURRENCY_PAYMENT_ORDER", "NATIONAL_PAYMENT",
//                        "INCOMING_CASH_ORDER", "EXPENSE_CASH_ORDER", "MEMORIAL_ORDER", "NOT_BALANCE_DEBIT_RUB",
//                        "NOT_BALANCE_CREDIT_RUB", "COMMISSION")).setTagTooltip("Тип платежного документа"),
//                new StringTagRule("roCode", 255, List.of("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18")),
//                new StringTagRule("payQueueCode", -1, List.of("1", "2", "3", "4", "5", "6")),
//                new TagRule("priority", Integer.class.getTypeName(), 10),
//                new StringTagRule("isDisabled", -1, List.of("0", "1")),
//                new StringTagRule("transactionCode", 50),
//                new StringTagRule("payPurpose", 255)
//        ));
//        put("corrAccountEventSideMeta", List.of(
//                new StringTagRule("corrAccountEventCode", 50),
//                new StringTagRule("sideCode", 255, List.of("Debit", "Credit")),
//                new StringTagRule("modelCode", 50),
//                new StringTagRule("accIntentionCode", 50),
//                new StringTagRule("amountOptionCode", 255, List.of("EventAmount", "AmountAfterConversion",
//                        "Method", "CommissionAmount", "ProcessAttribute", "DebitAmount", "CreditAmount", "TaxAmount", "CommissionAmountPlusTaxAmount"))
//        ));
//        put("changeSet", List.of(
//                new StringTagRule("comment", 255).setTagTooltip("Комментарий к ченджсету. Отображается только в колонке comments таблицы databaseChangeLog.")
//        ));
//    }};

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

//    public static final List<RequiredTagsRule> requiredTagsRules = new ArrayList<>(){{
//        add(new RequiredTagsRule("changeSet").setRequiredInnerTags(List.of("comment")));
//    }};

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
        if (rule.getMaxLength() != -1 && attributeText.length() > rule.getMaxLength()){
            Utils.registerError(holder, attribute, Localization.message(DeclinationHelper.CHARACTER_NOMINATIVE_ATTR.getLocaleKey(rule.getMaxLength()), rule.getMaxLength()));
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
        if (rule.getMaxLength() != -1 && tagText.length() > rule.getMaxLength()){
            Utils.registerErrorOnValueOrTag(holder, tag, Localization.message(DeclinationHelper.CHARACTER_NOMINATIVE_TAG.getLocaleKey(rule.getMaxLength()), rule.getMaxLength()));
        }

        // Проверка на обязательность
        if (tagText.isEmpty() && rule.isRequired()){
            Utils.registerErrorOnValueOrTag(holder, tag, Localization.message("tag.warn.required"));
            return;
        }

        if (!tagText.isEmpty()){
            // Проверка на целое число
            if (Long.class.getTypeName().equals(rule.getType())){
                try{
                    Long.parseLong(tagText);
                } catch (NumberFormatException e){
                    Utils.registerErrorOnValueOrTag(holder, tag, Localization.message("tag.warn.must-be-integer"));
                }
            }
            // Проверка на число с плавающей точкой
            if (Double.class.getTypeName().equals(rule.getType())){
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
        }

        // Проверка на допустимые значения
        // Не заполняются при type=Boolean
        if (!tagText.isEmpty()){
            List<String> availableValuesStrings = rule.getAvailableValues().stream().map(AvailableValue::getValue).toList();
            if (Utils.isNotEmpty(rule.getAvailableValues()) && !availableValuesStrings.contains(tagText)){
                Utils.registerErrorOnValueOrTag(holder, tag, Localization.message("tag.warn.must-be-following", availableValuesStrings));
            }
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

