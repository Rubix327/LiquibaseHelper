package me.rubix327.liquibasehelper.form.metadatagenerator;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.rubix327.liquibasehelper.Utils;
import me.rubix327.liquibasehelper.inspection.model.TagRule;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static me.rubix327.liquibasehelper.form.metadatagenerator.MetadataGeneratorUtils.convertMapToRow;

public class MetadataGeneratorTable extends JBTable {

    private final Project project;
    private final TagRule parentTagRule;
    private final String tableTagName;
    private final DefaultTableModel model;
    private ToolbarDecorator toolbarDecorator;

    public MetadataGeneratorTable(@NotNull Project project, @NotNull TagRule parentTagRule, String tableTagName, List<TagRule> rules) {
        this.project = project;
        this.parentTagRule = parentTagRule;
        this.tableTagName = tableTagName;
        Object[] columnNames = rules.stream().map(Utils::getTagLabel).toArray(String[]::new);
        this.model = new DefaultTableModel(columnNames, 0);
        init();
    }

    public JPanel getPanel(){
        return toolbarDecorator.createPanel();
    }

    @Override
    public DefaultTableModel getModel() {
        return model;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    public void init(){
        setName(Utils.getTagLabel(parentTagRule));
        setModel(this.model);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        toolbarDecorator = ToolbarDecorator.createDecorator(this);
    }

    /**
     * Добавить на таблицу кнопку добавления строки
     */
    public void makeAddAction(@NotNull MakeActionRequest request){
        toolbarDecorator.setAddAction(anActionButton -> {
            MetadataGenerator generator = new MetadataGenerator(project, tableTagName, null, request.getParentalIndex() + 1, map -> {
                model.addRow(convertMapToRow(map));
                List<Map<TagRule, Object>> existingList = request.getTableValues().getOrDefault(parentTagRule, new ArrayList<>());
                existingList.add(map);
                request.getTableValues().put(parentTagRule, existingList);
                request.getOnFinish().run();
            }).showWindow();
            request.getChildren().add(generator);
        });
    }

    /**
     * Добавить на таблицу кнопку удаления строки
     */
    public void makeRemoveAction(@NotNull MakeActionRequest request){
        toolbarDecorator.setRemoveAction(anActionButton -> {
            int selectedRow = getSelectedRow();
            if (selectedRow >= 0) {
                model.removeRow(selectedRow);
                request.getTableValues().get(parentTagRule).remove(selectedRow);
                request.getOnFinish().run();
            }
        });
    }

    /**
     * Добавить на таблицу кнопку редактирования строки
     */
    public void makeEditAction(@NotNull MakeActionRequest request){
        toolbarDecorator.addExtraAction(new AnAction("Edit", "Edit selected row", AllIcons.Actions.Edit) {
            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                // Включаем кнопку только когда выделена строка
                Presentation presentation = e.getPresentation();
                presentation.setEnabled(getSelectedRow() >= 0);
            }

            @Override
            public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                int selectedRow = getSelectedRow();
                if (selectedRow >= 0){
                    Map<TagRule, Object> thisTableRows = request.getTableValues().get(parentTagRule).get(selectedRow);
                    MetadataGenerator generator = new MetadataGenerator(project, tableTagName, thisTableRows, request.getParentalIndex() + 1, map -> {
                        model.removeRow(selectedRow);
                        model.insertRow(selectedRow, convertMapToRow(map));
                        request.getTableValues().get(parentTagRule).set(selectedRow, map);
                        request.getOnFinish().run();
                    }).showWindow();
                    request.getChildren().add(generator);
                }
            }
        });
    }

    /**
     * DTO-Запрос на добавление кнопки
     */
    @Getter
    @AllArgsConstructor
    public static final class MakeActionRequest{
        @NotNull private final Map<TagRule, List<Map<TagRule, Object>>> tableValues;
        @NotNull private final Runnable onFinish;
        private final int parentalIndex;
        private final List<MetadataGenerator> children;
    }

}
