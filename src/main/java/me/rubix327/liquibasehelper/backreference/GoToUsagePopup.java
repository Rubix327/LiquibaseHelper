package me.rubix327.liquibasehelper.backreference;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.PopupChooserBuilder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.JBUI;
import me.rubix327.liquibasehelper.Utils;
import me.rubix327.liquibasehelper.locale.Localization;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class GoToUsagePopup {

    private final Project project;

    public GoToUsagePopup(Project project) {
        this.project = project;
    }

    public void showUsagesPopup(List<PsiElement> usageFiles, Component anchorComponent) {
        if (usageFiles.isEmpty()) {
            JOptionPane.showMessageDialog(null, Localization.message("file.usages.not-found"));
            return;
        }

        if (usageFiles.size() == 1) {
            // У файла только один Usage, сразу открываем его
            Utils.openFile(project, usageFiles.get(0));
            return;
        }

        DefaultListModel<PsiElement> listModel = new DefaultListModel<>();
        usageFiles.forEach(listModel::addElement);

        JList<PsiElement> fileList = new JBList<>(listModel);
        fileList.setCellRenderer(new FileListCellRenderer());
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel();
            if (value != null) {
                String fullPath = value.getContainingFile().getVirtualFile().getPath();
                label.setText(Utils.getDisplayPathCutProject(project, fullPath));
            }

            label.setBorder(JBUI.Borders.empty(5));
            if (isSelected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
                label.setOpaque(true);
            }
            return label;
        });

        // Создаём popup для выбора файла
        JBPopup popup = new PopupChooserBuilder<>(fileList)
                .setTitle("Choose File to Go To")
                .setItemChoosenCallback(() -> {
                    PsiElement psiElement = fileList.getSelectedValue();
                    if (psiElement != null){
                        Utils.openFile(project, psiElement);
                    }
                })
                .setResizable(true)
                .setMovable(true)
                .createPopup();

        // Отображаем всплывающее окно под указанным компонентом
        popup.showUnderneathOf(anchorComponent);

    }

    // Рендерер для отображения имён файлов в списке
    private static class FileListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof VirtualFile) {
                setText(((VirtualFile) value).getPath());  // Показываем путь файла
            }
            setBorder(JBUI.Borders.empty(5));
            return component;
        }
    }
}
