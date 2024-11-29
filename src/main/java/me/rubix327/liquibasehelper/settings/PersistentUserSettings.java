package me.rubix327.liquibasehelper.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@State(name = "PersistentUserSettings", storages = @Storage("PersistentUserSettings.xml"))
public class PersistentUserSettings implements PersistentStateComponent<PersistentUserSettings> {
    public boolean enableReferences = true;
    public boolean enableBackReferences = true;
    public boolean enableNotLoadedNotifications = true;
    public boolean enableInspections = true;
    public boolean enableDocumentation = true;
    public boolean enableTagAutoCompletion = true;
    public boolean enableSettingsMenu = true;

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
    }

    public void resetDynamicSettings(){
        enableReferences = StaticSettings.ENABLE_REFERENCES;
        enableBackReferences = StaticSettings.ENABLE_BACK_REFERENCES;
        enableNotLoadedNotifications = StaticSettings.ENABLE_NOT_LOADED_NOTIFICATIONS;
        enableInspections = StaticSettings.ENABLE_INSPECTIONS;
        enableDocumentation = StaticSettings.ENABLE_DOCUMENTATION;
        enableTagAutoCompletion = StaticSettings.ENABLE_TAG_AUTO_COMPLETION;
        enableSettingsMenu = StaticSettings.ENABLE_SETTINGS_MENU;
    }

    @Override
    public String toString() {
        return "enableReferences=" + enableReferences +
                "\n enableBackReferences=" + enableBackReferences +
                "\n enableNotLoadedNotifications=" + enableNotLoadedNotifications +
                "\n enableInspections=" + enableInspections +
                "\n enableDocumentation=" + enableDocumentation +
                "\n enableTagAutoCompletion=" + enableTagAutoCompletion +
                "\n enableSettingsMenu=" + enableSettingsMenu;
    }
}
