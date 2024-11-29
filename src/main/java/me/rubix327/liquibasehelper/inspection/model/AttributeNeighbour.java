package me.rubix327.liquibasehelper.inspection.model;

public class AttributeNeighbour {

    public String attributeName;
    public String attributeValue;

    public AttributeNeighbour(String attributeName, String attributeValue) {
        this.attributeName = attributeName;
        this.attributeValue = attributeValue;
    }

    @Override
    public String toString() {
        return "AttributeNeighbour{" +
                "attributeName='" + attributeName + '\'' +
                ", attributeValue='" + attributeValue + '\'' +
                '}';
    }
}
