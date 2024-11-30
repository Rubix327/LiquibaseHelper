package me.rubix327.liquibasehelper.settings;

import com.intellij.codeInspection.ProblemHighlightType;
import me.rubix327.liquibasehelper.locale.Locale;

public class StaticSettings {

    public static final String PLUGIN_NAME = "LiquibaseHelper";
    public static final ProblemHighlightType ERRORS_HIGHLIGHT_TYPE = ProblemHighlightType.GENERIC_ERROR;
    public static boolean ENABLE_REFERENCES = true;
    public static boolean ENABLE_BACK_REFERENCES = true;
    public static boolean ENABLE_NOT_LOADED_NOTIFICATIONS = true;
    public static boolean ENABLE_INSPECTIONS = true;
    public static boolean ENABLE_DOCUMENTATION = true;
    public static boolean ENABLE_TAG_AUTO_COMPLETION = true;
    public static boolean ENABLE_SETTINGS_MENU = true;

    public static boolean SEARCH_ANNOTATION_BY_QUALIFIED_NAME = false;
    public static Locale LOCALE = Locale.RU;

}
