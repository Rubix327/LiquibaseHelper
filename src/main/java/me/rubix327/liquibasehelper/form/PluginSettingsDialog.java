package me.rubix327.liquibasehelper.form;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import me.rubix327.liquibasehelper.locale.Locale;
import me.rubix327.liquibasehelper.log.MainLogger;
import me.rubix327.liquibasehelper.settings.PersistentUserSettings;
import me.rubix327.liquibasehelper.settings.StaticSettings;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class PluginSettingsDialog extends DialogWrapper {

    private final Project project;
    private final PersistentUserSettings settings;

    public PluginSettingsDialog(Project project) {
        super(project, true);
        this.project = project;
        this.settings = project.getService(PersistentUserSettings.class);

        setTitle(StaticSettings.PLUGIN_NAME + " Settings");
        setSize(380, 320);
        setAutoAdjustable(false);
        setResizable(false);
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(8, 1, 2, 2));

        LJBCheckBox enableReferencesCheckbox = new LJBCheckBox("Навигация");
        enableReferencesCheckbox.setSelected(StaticSettings.ENABLE_REFERENCES);
        enableReferencesCheckbox.setEnabled(false);
        enableReferencesCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);

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

        // Комбобокс для выбора языка
        String[] locales = Arrays.stream(Locale.values()).map(Locale::name).toArray(String[]::new);
        JComboBox<String> languageComboBox = new ComboBox<>(locales);
        languageComboBox.setSelectedItem(StaticSettings.LOCALE.name());
        languageComboBox.addActionListener(e -> {
            String selectedLanguage = (String) languageComboBox.getSelectedItem();
            settings.locale = Locale.valueOf(selectedLanguage);
        });

        // Панель для выбора языка
        JPanel languagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        languagePanel.add(new JLabel("Язык:"));
        languagePanel.add(languageComboBox);

        panel.add(enableReferencesCheckbox);
        panel.add(enableBackReferencesCheckbox);
        panel.add(enableNotLoadedNotificationsCheckbox);
        panel.add(enableInspectionsCheckbox);
        panel.add(enableDocumentationCheckbox);
        panel.add(enableTagAutoCompletionCheckbox);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(languagePanel);

        return panel;
    }

    @Override
    protected void doOKAction() {
        MainLogger.info(project, "Updating settings:");
        settings.updateStaticSettings();
        for (String s : settings.toString().split("\n")) {
            MainLogger.info(project, 1, s);
        }

        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        settings.resetDynamicSettings();

        super.doCancelAction();
    }

}

