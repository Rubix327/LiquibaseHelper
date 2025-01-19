package me.rubix327.liquibasehelper.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import lombok.Getter;
import lombok.Setter;
import me.rubix327.liquibasehelper.locale.Locale;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@State(name = "LiquibaseHelperUserSettings", storages = @Storage("LiquibaseHelperUserSettings.xml"))
@Service(Service.Level.APP)
public final class PersistentUserSettings implements PersistentStateComponent<PersistentUserSettings> {
    public boolean enableReferences = true;
    public boolean enableBackReferences = true;
    public boolean enableNotLoadedNotifications = true;
    public boolean enableInspections = true;
    public boolean enableDocumentation = true;
    public boolean enableTagAutoCompletion = true;
    public boolean enableSettingsMenu = true;
    public boolean enableProcessVariablesCheck = true;
    public Locale locale = Locale.RU;

    public static PersistentUserSettings getInstance() {
        return ApplicationManager.getApplication().getService(PersistentUserSettings.class);
    }

    @Override
    public PersistentUserSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull PersistentUserSettings state) {
        XmlSerializerUtil.copyBean(state, this);
        updateStaticSettings();
    }

    public void updateStaticSettings(){
        StaticSettings.ENABLE_REFERENCES = enableReferences;
        StaticSettings.ENABLE_BACK_REFERENCES = enableBackReferences;
        StaticSettings.ENABLE_NOT_LOADED_NOTIFICATIONS = enableNotLoadedNotifications;
        StaticSettings.ENABLE_INSPECTIONS = enableInspections;
        StaticSettings.ENABLE_DOCUMENTATION = enableDocumentation;
        StaticSettings.ENABLE_TAG_AUTO_COMPLETION = enableTagAutoCompletion;
        StaticSettings.ENABLE_SETTINGS_MENU = enableSettingsMenu;
        StaticSettings.ENABLE_PROCESS_VARIABLES_CHECK = enableProcessVariablesCheck;
        changeLanguage(locale);
    }

    public void changeLanguage(Locale locale){
        StaticSettings.LOCALE = locale;
        java.util.Locale.setDefault(new java.util.Locale(locale.getName()));
    }

    public boolean isSettingsModified(){
        return  enableReferences != StaticSettings.ENABLE_REFERENCES ||
                enableBackReferences != StaticSettings.ENABLE_BACK_REFERENCES ||
                enableNotLoadedNotifications != StaticSettings.ENABLE_NOT_LOADED_NOTIFICATIONS ||
                enableInspections != StaticSettings.ENABLE_INSPECTIONS ||
                enableDocumentation != StaticSettings.ENABLE_DOCUMENTATION ||
                enableTagAutoCompletion != StaticSettings.ENABLE_TAG_AUTO_COMPLETION ||
                enableSettingsMenu != StaticSettings.ENABLE_SETTINGS_MENU ||
                enableProcessVariablesCheck != StaticSettings.ENABLE_PROCESS_VARIABLES_CHECK ||
                locale != StaticSettings.LOCALE;
    }

    public void resetDynamicSettings(){
        enableReferences = StaticSettings.ENABLE_REFERENCES;
        enableBackReferences = StaticSettings.ENABLE_BACK_REFERENCES;
        enableNotLoadedNotifications = StaticSettings.ENABLE_NOT_LOADED_NOTIFICATIONS;
        enableInspections = StaticSettings.ENABLE_INSPECTIONS;
        enableDocumentation = StaticSettings.ENABLE_DOCUMENTATION;
        enableTagAutoCompletion = StaticSettings.ENABLE_TAG_AUTO_COMPLETION;
        enableSettingsMenu = StaticSettings.ENABLE_SETTINGS_MENU;
        enableProcessVariablesCheck = StaticSettings.ENABLE_PROCESS_VARIABLES_CHECK;
        locale = StaticSettings.LOCALE;
    }

    @Override
    public String toString() {
        return "enableReferences=" + enableReferences +
                "\nenableBackReferences=" + enableBackReferences +
                "\nenableNotLoadedNotifications=" + enableNotLoadedNotifications +
                "\nenableInspections=" + enableInspections +
                "\nenableDocumentation=" + enableDocumentation +
                "\nenableTagAutoCompletion=" + enableTagAutoCompletion +
                "\nenableSettingsMenu=" + enableSettingsMenu +
                "\nenableProcessVariablesCheck=" + enableProcessVariablesCheck +
                "\nlocale=" + locale.getName();
    }
}
