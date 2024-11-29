package me.rubix327.liquibasehelper.inspection.model;

import lombok.Getter;
import me.rubix327.liquibasehelper.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Getter
public class AvailableValue {

    private final String value;
    private String comment;

    public AvailableValue(String value) {
        this.value = value;
    }

    public AvailableValue(String value, String comment) {
        this.value = value;
        this.comment = comment;
    }

    @NotNull
    public static List<AvailableValue> stringToAvailableValues(String s){
        // В строке нет точки с запятой
        if (!s.contains(";")){
            AvailableValue av = singleStringToAvailableValue(s);
            if (av == null) return new ArrayList<>();
            return new ArrayList<>(List.of(av));
        }

        if (s.trim().length() == 1) return new ArrayList<>(); // В строке только ТСЗ
        if (s.trim().startsWith(";")) return new ArrayList<>(); // Строка начинается с ТСЗ

        String[] valuesAndComments = s.split(";");
        return Arrays.stream(valuesAndComments).map(AvailableValue::singleStringToAvailableValue).filter(Objects::nonNull).toList();
    }

    @Nullable
    private static AvailableValue singleStringToAvailableValue(@NotNull String s){
        if (s.isEmpty()) return null; // Строка пустая
        if (!s.contains(":")) return new AvailableValue(s); // В строке нет двоеточия - не надо разделять
        if (s.trim().length() == 1) return null; // В строке только двоеточие и больше ничего
        if (s.trim().startsWith(":")) return null; // Строка начинается с двоеточия - значит значение не задано

        String[] valueAndComment = s.split(":");

        // В строке не задано значение справа
        // Пример: "Currency:",
        if (valueAndComment.length == 1) {
            return new AvailableValue(s.replace(":", ""));
        }

        // Хорошая строка, разделенная двоеточием. Пример: "0:yes"
        return new AvailableValue(valueAndComment[0], valueAndComment[1]);
    }

    @Override
    public String toString() {
        if (Utils.isNotBlank(comment)){
            return value + ":" + comment;
        }
        return value;
    }

}
