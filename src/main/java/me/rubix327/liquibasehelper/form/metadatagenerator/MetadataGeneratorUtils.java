package me.rubix327.liquibasehelper.form.metadatagenerator;

import me.rubix327.liquibasehelper.inspection.model.TagRule;
import me.rubix327.liquibasehelper.locale.DeclinationHelper;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MetadataGeneratorUtils {

    public static void sortRules(List<TagRule> rules){
        rules.sort((o1, o2) -> {
            if (o1.isRequired() && !o2.isRequired()){
                return -1;
            } else if (o2.isRequired() && !o1.isRequired()){
                return 1;
            } else {
                return 0;
            }
        });
    }

    public static String[] convertMapToRow(LinkedHashMap<?, ?> map){
        List<String> result = new ArrayList<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getValue() instanceof String s){
                result.add(s);
            } else if (entry.getValue() instanceof List l){
                result.add(DeclinationHelper.ELEMENT_NOMINATIVE.message(l.size()));
            } else if (entry.getValue() == null){
                result.add(null);
            }
        }
        return result.toArray(String[]::new);
    }

    public static DocumentListener getTextFieldListener(Runnable runnable){
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                runnable.run();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                runnable.run();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                runnable.run();
            }
        };
    }

}
