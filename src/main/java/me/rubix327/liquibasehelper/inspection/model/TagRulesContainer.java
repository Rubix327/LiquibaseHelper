package me.rubix327.liquibasehelper.inspection.model;

import com.google.common.base.Objects;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
public class TagRulesContainer {

    private String parentTagName;
    private String parentTagTooltip;
    private String parentTagDescription;
    private String metaClassPath;
    private int metaClassNameOffset = 0;
    private List<TagRule> tagRules;

    public String getLinkToMetaClassWithOffset(){
        return metaClassPath + ":" + metaClassNameOffset;
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

    public TagRulesContainer setClassPath(String metaClassPath) {
        this.metaClassPath = metaClassPath;
        return this;
    }

    public TagRulesContainer setClassNameOffset(int classNameOffset) {
        this.metaClassNameOffset = classNameOffset;
        return this;
    }

    public TagRulesContainer setTagRules(List<TagRule> tagRules) {
        this.tagRules = tagRules;
        return this;
    }

    @Override
    public String toString() {
        return "parentTagTooltip='" + parentTagTooltip + '\'' +
                ", classPath='" + metaClassPath + '\'' +
                ", tagRules=" + tagRules;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        TagRulesContainer container = (TagRulesContainer) object;
        return Objects.equal(parentTagName, container.parentTagName) && Objects.equal(metaClassPath, container.metaClassPath);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(parentTagName, metaClassPath);
    }
}
