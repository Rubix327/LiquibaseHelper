package me.rubix327.liquibasehelper.settings;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class PluginSettingsDialog extends DialogWrapper {

    private final PersistentUserSettings settings;

    public PluginSettingsDialog(Project project) {
        super(project, true);
        this.settings = project.getService(PersistentUserSettings.class);

        setTitle(StaticSettings.PLUGIN_NAME + " Settings");
        setSize(100, 200);
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(10, 1, 5, 5));

        LJBCheckBox enableReferencesCheckbox = new LJBCheckBox("Навигация");
        enableReferencesCheckbox.setSelected(StaticSettings.ENABLE_REFERENCES);
        enableReferencesCheckbox.setEnabled(false);

        LJBCheckBox enableNotLoadedNotificationsCheckbox = new LJBCheckBox("Оповещения о незагруженных файлах/ченджсетах");
        enableNotLoadedNotificationsCheckbox.setSelected(StaticSettings.ENABLE_NOT_LOADED_NOTIFICATIONS);
        enableNotLoadedNotificationsCheckbox.setEnabled(StaticSettings.ENABLE_BACK_REFERENCES && StaticSettings.ENABLE_NOT_LOADED_NOTIFICATIONS);
        enableNotLoadedNotificationsCheckbox.addLChangeListener(e -> settings.setEnableNotLoadedNotifications(e.isNowSelected()));

        LJBCheckBox enableBackReferencesCheckbox = new LJBCheckBox("Обратная навигация");
        enableBackReferencesCheckbox.setSelected(StaticSettings.ENABLE_BACK_REFERENCES);
        enableBackReferencesCheckbox.setEnabled(StaticSettings.ENABLE_REFERENCES);
        enableBackReferencesCheckbox.addLChangeListener(e -> {
            settings.setEnableBackReferences(e.isNowSelected());
            enableNotLoadedNotificationsCheckbox.setEnabled(e.isNowSelected());

            if (e.wasSelected()){
                enableNotLoadedNotificationsCheckbox.setSelected(false);
            }
        });

        LJBCheckBox enableDocumentationCheckbox = new LJBCheckBox("Подсказки при наведении на теги");
        enableDocumentationCheckbox.setSelected(StaticSettings.ENABLE_DOCUMENTATION);
        enableDocumentationCheckbox.setEnabled(StaticSettings.ENABLE_INSPECTIONS);
        enableDocumentationCheckbox.addLChangeListener(e -> settings.setEnableDocumentation(e.isNowSelected()));

        LJBCheckBox enableTagAutoCompletionCheckbox = new LJBCheckBox("Подсказки для автозаполнения");
        enableTagAutoCompletionCheckbox.setSelected(StaticSettings.ENABLE_TAG_AUTO_COMPLETION);
        enableTagAutoCompletionCheckbox.setEnabled(StaticSettings.ENABLE_INSPECTIONS);
        enableTagAutoCompletionCheckbox.addLChangeListener(e -> settings.setEnableTagAutoCompletion(e.isNowSelected()));

        LJBCheckBox enableInspectionsCheckbox = new LJBCheckBox("Валидация XML-кода");
        enableInspectionsCheckbox.setSelected(StaticSettings.ENABLE_INSPECTIONS);
        enableInspectionsCheckbox.addLChangeListener(e -> {
            settings.setEnableInspections(e.isNowSelected());
            enableDocumentationCheckbox.setEnabled(e.isNowSelected());
            enableTagAutoCompletionCheckbox.setEnabled(e.isNowSelected());

            if (e.wasSelected()){
                enableDocumentationCheckbox.setSelected(false);
                enableTagAutoCompletionCheckbox.setSelected(false);
            }
        });

        panel.add(enableReferencesCheckbox);
        panel.add(enableBackReferencesCheckbox);
        panel.add(enableNotLoadedNotificationsCheckbox);
        panel.add(enableInspectionsCheckbox);
        panel.add(enableDocumentationCheckbox);
        panel.add(enableTagAutoCompletionCheckbox);

        return panel;
    }

    @Override
    protected void doOKAction() {
        settings.updateStaticSettings();

        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        settings.resetDynamicSettings();

        super.doCancelAction();
    }

}

