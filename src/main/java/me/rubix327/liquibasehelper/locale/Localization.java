package me.rubix327.liquibasehelper.locale;

import com.intellij.AbstractBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.text.MessageFormat;

public class Localization extends AbstractBundle {

    private static final String BUNDLE = "messages.messages";
    private static final Localization INSTANCE = new Localization();

    public Localization() {
        super(BUNDLE);
    }

    public static @NotNull String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return MessageFormat.format(INSTANCE.getMessage(key), params);
    }

}
