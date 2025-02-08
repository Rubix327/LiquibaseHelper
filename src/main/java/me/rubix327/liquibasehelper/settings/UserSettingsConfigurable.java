package me.rubix327.liquibasehelper.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.ComboBox;
import me.rubix327.liquibasehelper.form.LJBCheckBox;
import me.rubix327.liquibasehelper.locale.Locale;
import me.rubix327.liquibasehelper.locale.Localization;
import me.rubix327.liquibasehelper.log.MainLogger;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class UserSettingsConfigurable implements Configurable {

    private final PersistentUserSettings settings;

    private LJBCheckBox enableReferencesCheckbox;
    private LJBCheckBox enableNotLoadedNotificationsCheckbox;
    private LJBCheckBox enableBackReferencesCheckbox;
    private LJBCheckBox enableInspectionsCheckbox;
    private LJBCheckBox enableDocumentationCheckbox;
    private LJBCheckBox enableTagAutoCompletionCheckbox;
    private LJBCheckBox enableProcessVariablesCheckCheckbox;
    private JComboBox<String> languageComboBox;
    private JLabel languageLabel;
    private JLabel restartRequiredNotification;

    public UserSettingsConfigurable() {
        this.settings = PersistentUserSettings.getInstance();
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return StaticSettings.getPluginName();
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        MainLogger.info("Showing plugin settings in common menu.");
        return createCenterPanel();
    }

    @Override
    public boolean isModified() {
        // Проверяем, изменены ли настройки
        return settings.isSettingsModified();
    }

    @Override
    public void apply() {
        // Применяем изменения
        MainLogger.info("Updating settings:");
        settings.updateStaticSettings();
        for (String s : settings.toString().split("\n")) {
            MainLogger.info(1, s);
        }
    }

    @Override
    public void reset() {
        // Сбрасываем изменения
        settings.resetDynamicSettings();
        enableReferencesCheckbox.setSelected(StaticSettings.ENABLE_REFERENCES);
        enableBackReferencesCheckbox.setSelected(StaticSettings.ENABLE_BACK_REFERENCES);
        enableNotLoadedNotificationsCheckbox.setSelected(StaticSettings.ENABLE_NOT_LOADED_NOTIFICATIONS);
        enableInspectionsCheckbox.setSelected(StaticSettings.ENABLE_INSPECTIONS);
        enableDocumentationCheckbox.setSelected(StaticSettings.ENABLE_DOCUMENTATION);
        enableTagAutoCompletionCheckbox.setSelected(StaticSettings.ENABLE_TAG_AUTO_COMPLETION);
        enableProcessVariablesCheckCheckbox.setSelected(StaticSettings.ENABLE_PROCESS_VARIABLES_CHECK);
        languageComboBox.setSelectedItem(StaticSettings.LOCALE.name());
    }

    @Override
    public void disposeUIResources() {
        enableReferencesCheckbox = null;
        enableBackReferencesCheckbox = null;
        enableNotLoadedNotificationsCheckbox = null;
        enableInspectionsCheckbox = null;
        enableDocumentationCheckbox = null;
        enableTagAutoCompletionCheckbox = null;
        languageComboBox = null;
    }

    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 1));

        enableReferencesCheckbox = new LJBCheckBox(Localization.message("settings.enableReferences"));
        enableReferencesCheckbox.setSelected(StaticSettings.ENABLE_REFERENCES);
        enableReferencesCheckbox.setEnabled(false);
        enableReferencesCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);

        enableNotLoadedNotificationsCheckbox = new LJBCheckBox(Localization.message("settings.enableNotLoadedNotifications"));
        enableNotLoadedNotificationsCheckbox.setSelected(StaticSettings.ENABLE_NOT_LOADED_NOTIFICATIONS);
        enableNotLoadedNotificationsCheckbox.setEnabled(StaticSettings.ENABLE_BACK_REFERENCES && StaticSettings.ENABLE_NOT_LOADED_NOTIFICATIONS);
        enableNotLoadedNotificationsCheckbox.addLChangeListener(e -> settings.setEnableNotLoadedNotifications(e.isNowSelected()));

        enableBackReferencesCheckbox = new LJBCheckBox(Localization.message("settings.enableBackReferences"));
        enableBackReferencesCheckbox.setSelected(StaticSettings.ENABLE_BACK_REFERENCES);
        enableBackReferencesCheckbox.setEnabled(StaticSettings.ENABLE_REFERENCES);
        enableBackReferencesCheckbox.addLChangeListener(e -> {
            settings.setEnableBackReferences(e.isNowSelected());
            enableNotLoadedNotificationsCheckbox.setEnabled(e.isNowSelected());

            if (e.wasSelected()){
                enableNotLoadedNotificationsCheckbox.setSelected(false);
            }
        });

        enableDocumentationCheckbox = new LJBCheckBox(Localization.message("settings.enableDocumentation"));
        enableDocumentationCheckbox.setSelected(StaticSettings.ENABLE_DOCUMENTATION);
        enableDocumentationCheckbox.setEnabled(StaticSettings.ENABLE_INSPECTIONS);
        enableDocumentationCheckbox.addLChangeListener(e -> settings.setEnableDocumentation(e.isNowSelected()));

        enableTagAutoCompletionCheckbox = new LJBCheckBox(Localization.message("settings.enableTagAutoCompletion"));
        enableTagAutoCompletionCheckbox.setSelected(StaticSettings.ENABLE_TAG_AUTO_COMPLETION);
        enableTagAutoCompletionCheckbox.setEnabled(StaticSettings.ENABLE_INSPECTIONS);
        enableTagAutoCompletionCheckbox.addLChangeListener(e -> settings.setEnableTagAutoCompletion(e.isNowSelected()));

        enableInspectionsCheckbox = new LJBCheckBox(Localization.message("settings.enableInspections"));
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

        enableProcessVariablesCheckCheckbox = new LJBCheckBox(Localization.message("settings.enableProcessVariablesCheck"));
        enableProcessVariablesCheckCheckbox.setSelected(StaticSettings.ENABLE_PROCESS_VARIABLES_CHECK);
        enableProcessVariablesCheckCheckbox.addLChangeListener(e -> settings.setEnableProcessVariablesCheck(e.isNowSelected()));

        // Комбобокс для выбора языка
        String[] locales = Arrays.stream(Locale.values()).map(Locale::name).toArray(String[]::new);
        languageComboBox = new ComboBox<>(locales);
        languageComboBox.setSelectedItem(StaticSettings.LOCALE.name());
        languageComboBox.addActionListener(e -> {
            String selectedLanguage = (String) languageComboBox.getSelectedItem();
            settings.locale = Locale.valueOf(selectedLanguage);
            onLanguageChanged(settings.locale);
        });

        // Панель для выбора языка
        JPanel languagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        languageLabel = new JLabel(Localization.message("settings.language"));
        languagePanel.add(languageLabel);
        languagePanel.add(languageComboBox);

        // Уведомление о необходимости перезагрузки IDE для полной смены языка
        restartRequiredNotification = new JLabel("");
        JPanel notificationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        notificationPanel.add(restartRequiredNotification);

        panel.add(enableReferencesCheckbox);
        panel.add(enableBackReferencesCheckbox);
        panel.add(enableNotLoadedNotificationsCheckbox);
        panel.add(enableInspectionsCheckbox);
        panel.add(enableDocumentationCheckbox);
        panel.add(enableTagAutoCompletionCheckbox);
        panel.add(enableProcessVariablesCheckCheckbox);
        panel.add(Box.createRigidArea(new Dimension(0, 2)));
        panel.add(languagePanel);
        panel.add(Box.createRigidArea(new Dimension(0, 2)));
        panel.add(notificationPanel);

        JPanel outerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        outerPanel.add(panel);
        return outerPanel;
    }

    private void onLanguageChanged(Locale locale){
        enableReferencesCheckbox.setText(Localization.message("settings.enableReferences", locale));
        enableBackReferencesCheckbox.setText(Localization.message("settings.enableBackReferences", locale));
        enableNotLoadedNotificationsCheckbox.setText(Localization.message("settings.enableNotLoadedNotifications", locale));
        enableInspectionsCheckbox.setText(Localization.message("settings.enableInspections", locale));
        enableDocumentationCheckbox.setText(Localization.message("settings.enableDocumentation", locale));
        enableTagAutoCompletionCheckbox.setText(Localization.message("settings.enableTagAutoCompletion", locale));
        enableProcessVariablesCheckCheckbox.setText(Localization.message("settings.enableProcessVariablesCheck", locale));
        languageLabel.setText(Localization.message("settings.language", locale));
        restartRequiredNotification.setText(settings.locale != StaticSettings.LOCALE ? Localization.message("settings.language.restart-required", locale) : "");
    }

}
