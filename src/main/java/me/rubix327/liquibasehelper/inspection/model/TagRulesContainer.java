package me.rubix327.liquibasehelper.inspection.model;

import lombok.Getter;

import java.util.List;

@Getter
public class TagRulesContainer {

    private String parentTagName;
    private String parentTagTooltip;
    private String parentTagDescription;
    private String linkToMetaClass;
    private int linkToClassNameOffset = 0;
    private List<TagRule> tagRules;

    public String getLinkToMetaClass() {
        return linkToMetaClass + ":" + linkToClassNameOffset;
    }

    public TagRulesContainer setParentTagName(String parentTagName) {
        this.parentTagName = parentTagName;
        return this;
    }

    public TagRulesContainer setParentTagTooltip(String parentTagTooltip) {
        this.parentTagTooltip = parentTagTooltip;
        return this;
    }

    public TagRulesContainer setParentTagDescription(String parentTagDescription) {
        this.parentTagDescription = parentTagDescription;
        return this;
    }

    public TagRulesContainer setLinkToMetaClass(String linkToMetaClass) {
        this.linkToMetaClass = linkToMetaClass;
        return this;
    }

    public TagRulesContainer setLinkToClassNameOffset(int linkToClassNameOffset) {
        this.linkToClassNameOffset = linkToClassNameOffset;
        return this;
    }

    public TagRulesContainer setTagRules(List<TagRule> tagRules) {
        this.tagRules = tagRules;
        return this;
    }

    @Override
    public String toString() {
        return "parentTagTooltip='" + parentTagTooltip + '\'' +
                ", metaClassPath='" + linkToMetaClass + '\'' +
                ", tagRules=" + tagRules;
    }
}
