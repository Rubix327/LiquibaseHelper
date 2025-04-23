package me.rubix327.liquibasehelper.form.metadatagenerator;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import me.rubix327.liquibasehelper.inspection.RulesManager;
import me.rubix327.liquibasehelper.inspection.model.TagRulesContainer;
import me.rubix327.liquibasehelper.locale.Localization;
import me.rubix327.liquibasehelper.settings.StaticSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

public class MetadataGeneratorChooseDialog extends DialogWrapper {

    private final Project project;
    private final Collection<TagRulesContainer> rules;
    private TagRulesContainer selectedTag;
    private boolean okClose;

    public MetadataGeneratorChooseDialog(@NotNull Project project) {
        super(project, false);
        this.project = project;
        this.rules = RulesManager.getInstance(project).getAllRulesContainers();

        setTitle(Localization.message("metadata-generator.choose-tag-window.title", StaticSettings.getPluginName()));
        setResizable(false);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        setResizable(false);

        TagRulesContainer[] rulesTags = rules.toArray(TagRulesContainer[]::new);

        if (rulesTags.length == 0){
            this.okClose = true;
            panel.add(new JLabel("В этом проекте нет ни одного @CbsDatamodelClass"), BorderLayout.CENTER);
            JPanel outerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            outerPanel.add(panel);
            pack();
            return outerPanel;
        }

        ComboBox<TagRulesContainer> comboBox = new ComboBox<>(rulesTags);
        comboBox.setSize(new Dimension(100, 35));
        this.selectedTag = rulesTags[0];
        comboBox.addActionListener(e -> {
            TagRulesContainer selectedItem = (TagRulesContainer) comboBox.getSelectedItem();
            if (selectedItem != null){
                this.selectedTag = selectedItem;
            }
        });
        comboBox.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JBLabel availableValueLabel = new JBLabel();
            if (value != null) {
                if (value.getParentTagTooltip() != null){
                    availableValueLabel.setText(value.getParentTagName() + " - " + value.getParentTagTooltip());
                } else {
                    availableValueLabel.setText(value.getParentTagName());
                }
            }
            return availableValueLabel;
        });

        panel.add(comboBox, BorderLayout.CENTER);
        JPanel outerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        outerPanel.add(panel);

        pack();
        return outerPanel;
    }

    @Override
    protected void doOKAction() {
        if (okClose){
            dispose();
            return;
        }
        if (project == null || selectedTag == null){
            return;
        }
        dispose();
        new MetadataGenerator(project, selectedTag.getParentTagName()).showWindow();
    }
}
