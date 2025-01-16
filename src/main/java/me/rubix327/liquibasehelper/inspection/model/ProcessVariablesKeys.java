package me.rubix327.liquibasehelper.inspection.model;

import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@ToString
public class ProcessVariablesKeys {

    Set<String> input = new HashSet<>();
    Set<String> output = new HashSet<>();

    public void addInput(String inputKey){
        input.add(inputKey);
    }

    public void addOutput(String outputKey){
        output.add(outputKey);
    }

    public void addUndefined(String undefinedKey){
        input.add(undefinedKey);
        output.add(undefinedKey);
    }

    public void add(String key, String direction){
        if ("INPUT".equals(direction)){
            addInput(key);
        }
        else if ("OUTPUT".equals(direction)){
            addOutput(key);
        }
        else if ("UNDEFINED".equals(direction)){
            addUndefined(key);
        }
    }

    public boolean hasInput(String inputKey){
        return input.contains(inputKey);
    }

    public boolean hasOutput(String outputKey){
        return output.contains(outputKey);
    }

    public void merge(ProcessVariablesKeys processVariablesKeys){
        this.input.addAll(processVariablesKeys.input);
        this.output.addAll(processVariablesKeys.output);
    }

    public String toShortString() {
        return "{" +
                "input=" + input +
                ", output=" + output +
                '}';
    }

}
