package me.rubix327.liquibasehelper.inspection.model;

import com.google.common.base.Objects;
import com.intellij.psi.xml.XmlAttribute;
import me.rubix327.liquibasehelper.inspection.XmlTagValuesInspector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public class AttributeRule {

    // Definers
    public String attributeName;
    public List<AttributeNeighbour> mustNeighbours = new ArrayList<>();
    public String mustTagName;
    public String mustParentName;
    public String mustGrandParentName;
    public String mustRootTagName;

    // Constraints
    public int maxLength = -1;
    public List<String> availableValues = new ArrayList<>();

    // Info
    public String attributeTooltip;

    public AttributeRule(String attributeName) {
        this.attributeName = attributeName;
    }

    public static AttributeRule getSuitableAttributeRule(List<AttributeRule> allRules, @NotNull XmlAttribute attribute){
        for (AttributeRule rule : allRules) {
            if (!attribute.getName().equals(rule.attributeName)) continue;
            if (rule.mustTagName != null){
                if (attribute.getParent() == null) continue;
                if (!rule.mustTagName.equals(attribute.getParent().getName())) continue;
            }
            if (rule.mustParentName != null){
                if (attribute.getParent() == null) continue;
                if (attribute.getParent().getParentTag() == null) continue;
                if (!rule.mustParentName.equals(attribute.getParent().getParentTag().getName())) continue;
            }
            if (!XmlTagValuesInspector.isTagSuitableByNeighbours(attribute.getParent(), rule.mustNeighbours)) continue;
            return rule;
        }
        return null;
    }

    public AttributeRule setAttributeName(String attributeName) {
        this.attributeName = attributeName;
        return this;
    }

    public AttributeRule setMustNeighbours(List<AttributeNeighbour> mustNeighbours) {
        this.mustNeighbours = mustNeighbours;
        return this;
    }

    public AttributeRule setMustTagName(String mustTagName) {
        this.mustTagName = mustTagName;
        return this;
    }

    public AttributeRule setMustParentName(String mustParentName) {
        this.mustParentName = mustParentName;
        return this;
    }

    public AttributeRule setMustGrandParentName(String mustGrandParentName) {
        this.mustGrandParentName = mustGrandParentName;
        return this;
    }

    public AttributeRule setMustRootTagName(String mustRootTagName) {
        this.mustRootTagName = mustRootTagName;
        return this;
    }

    public AttributeRule setMaxLength(int maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    public AttributeRule setAvailableValues(List<String> availableValues) {
        this.availableValues = availableValues;
        return this;
    }

    public AttributeRule setAttributeTooltip(String attributeTooltip) {
        this.attributeTooltip = attributeTooltip;
        return this;
    }

    public static void checkForDuplicates(List<AttributeRule> rules){
        Set<AttributeRule> elements = new HashSet<>();
        List<AttributeRule> duplicates = rules.stream()
                .filter(n -> !elements.add(n))
                .toList();
        if (!duplicates.isEmpty()){
            throw new RuntimeException("Attribute Rules list contains duplicates: " + duplicates);
        }
    }

    @Override
    public String toString() {
        return "AttributeConstraint{" +
                "attributeName='" + attributeName + '\'' +
                ", mustNeighbours=" + mustNeighbours +
                ", mustTagName='" + mustTagName + '\'' +
                ", mustParentTagName='" + mustGrandParentName + '\'' +
                ", mustRootTagName='" + mustRootTagName + '\'' +
                ", maxLength=" + maxLength +
                ", availableValues=" + availableValues +
                '}';
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        AttributeRule that = (AttributeRule) object;
        return Objects.equal(attributeName, that.attributeName) && Objects.equal(mustNeighbours, that.mustNeighbours) && Objects.equal(mustTagName, that.mustTagName) && Objects.equal(mustParentName, that.mustParentName) && Objects.equal(mustGrandParentName, that.mustGrandParentName) && Objects.equal(mustRootTagName, that.mustRootTagName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(attributeName, mustNeighbours, mustTagName, mustParentName, mustGrandParentName, mustRootTagName);
    }

}
