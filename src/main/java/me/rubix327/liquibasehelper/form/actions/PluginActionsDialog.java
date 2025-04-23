package me.rubix327.liquibasehelper.form.actions;

import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import me.rubix327.liquibasehelper.StartProjectComponent;
import me.rubix327.liquibasehelper.form.metadatagenerator.MetadataGeneratorChooseDialog;
import me.rubix327.liquibasehelper.locale.Localization;
import me.rubix327.liquibasehelper.log.MainLogger;
import me.rubix327.liquibasehelper.settings.StaticSettings;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class PluginActionsDialog extends DialogWrapper {

    private final Project project;

    public PluginActionsDialog(Project project) {
        super(project, true);
        this.project = project;

        setTitle(Localization.message("actions.title", StaticSettings.getPluginName()));
        setResizable(false);
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(8, 1, 2, 2));

        JButton reloadRulesButton = new JButton(Localization.message("actions.reload-rules"));
        reloadRulesButton.addActionListener((event) -> {
            MainLogger.info(project, "Clicked 'Reload rules' button in Actions menu...");
            StartProjectComponent.registerRulesForAllClassesAfterIndexingInBackground(project);
        });

        JButton changesetTreeButton = new JButton(Localization.message("actions.changeset-tree"));
        changesetTreeButton.setEnabled(false); // TODO

        JButton viewProblemsButton = new JButton(Localization.message("actions.find-problems"));
        viewProblemsButton.setEnabled(false); // TODO

        JButton metadataGeneratorButton = new JButton(Localization.message("actions.metadata-generator"));
        metadataGeneratorButton.addActionListener((event) -> {
            close(0);
            MainLogger.info(project, "Clicked 'Metadata generator...' button in Actions menu...");
            new MetadataGeneratorChooseDialog(project).show();
        });

        JButton openSettingsButton = new JButton(Localization.message("actions.open-settings"));
        openSettingsButton.addActionListener((event) -> {
            close(0);
            MainLogger.info(project, "Clicked 'Open plugin settings' button in Actions menu...");
            ShowSettingsUtil.getInstance().showSettingsDialog(project, StaticSettings.getPluginName());
        });

        panel.add(reloadRulesButton);
        panel.add(changesetTreeButton);
        panel.add(viewProblemsButton);
        panel.add(metadataGeneratorButton);
        panel.add(openSettingsButton);

        JPanel outerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        outerPanel.add(panel);
        return outerPanel;
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
    }

}
