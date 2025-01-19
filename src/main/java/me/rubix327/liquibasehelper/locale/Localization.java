package me.rubix327.liquibasehelper.locale;

import com.intellij.AbstractBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public class Localization extends AbstractBundle {

    private static final String BUNDLE = "messages.messages";
    private static final Localization INSTANCE = new Localization();

    public Localization() {
        super(BUNDLE);
    }

    public static @NotNull String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return MessageFormat.format(INSTANCE.getMessage(key), params);
    }
    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Locale locale, Object... params){
        String localeName = locale.getName();
        if (locale == Locale.EN){
            localeName = ""; // english is default, no extensions
        }
        ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE, new java.util.Locale(localeName));
        return MessageFormat.format(bundle.getString(key), params);
    }

}
