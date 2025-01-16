package me.rubix327.liquibasehelper.inspection.model;

import com.intellij.psi.xml.XmlTag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@RequiredArgsConstructor
public class TagRule {

    // Definers
    private final String tagName;

    // Constraints
    private String type;
    private int maxLength = 0;
    private boolean isRequired;
    private List<AvailableValue> availableValues = new ArrayList<>();

    // Info
    private String tagTooltip;
    private String tagDescription;
    private boolean extendedTooltipInfo = true;
    private String metaClassPath;
    private int metaFieldOffset = 0;

    public static TagRule getSuitableTagRule(@NotNull List<TagRule> rules, @NotNull XmlTag tag){
        for (TagRule rule : rules) {
            if (rule.tagName.equals(tag.getName())) return rule;
        }
        return null;
    }

    public String getLinkToMetaFieldWithOffset(){
        return metaClassPath + ":" + metaFieldOffset;
    }

    public static void sortByImportance(List<TagRule> tagRules){
        tagRules.sort((o1, o2) -> {
            if (o1.isRequired == o2.isRequired){
                return Integer.compare(tagRules.indexOf(o1), tagRules.indexOf(o2));
            }
            if (o1.isRequired) return -1;
            return 1;
        });
    }

    @Override
    public String toString() {
        return "TagRule{" +
                "tagName='" + tagName + '\'' +
                ", type='" + type + '\'' +
                ", maxLength=" + maxLength +
                ", required=" + isRequired +
                ", availableValues=" + availableValues +
                ", tagTooltip='" + tagTooltip + '\'' +
                ", classPath='" + metaClassPath + '\'' +
                ", fieldOffset=" + metaFieldOffset +
                '}';
    }
}
