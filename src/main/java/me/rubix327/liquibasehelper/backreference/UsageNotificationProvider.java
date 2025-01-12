package me.rubix327.liquibasehelper.backreference;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotificationProvider;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import me.rubix327.liquibasehelper.StartProjectComponent;
import me.rubix327.liquibasehelper.Utils;
import me.rubix327.liquibasehelper.locale.Localization;
import me.rubix327.liquibasehelper.settings.StaticSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Function;

public class UsageNotificationProvider implements EditorNotificationProvider {

    @Override
    public @Nullable Function<? super @NotNull FileEditor, ? extends @Nullable JComponent> collectNotificationData(@NotNull Project project, @NotNull VirtualFile file) {
        return (Function<FileEditor, JComponent>) fileEditor -> {
            // Проверяем, что файл находится внутри datamodel/resources и имеет расширение .xml
            if (!file.getPath().contains("datamodel") || !file.getPath().contains("resources") || !"xml".equalsIgnoreCase(file.getExtension())){
                return null;
            }

            if (!StaticSettings.ENABLE_BACK_REFERENCES && !StaticSettings.ENABLE_NOT_LOADED_NOTIFICATIONS){
                return null;
            }

            List<PsiElement> usages = UsageFinderUtil.findUsages(project, file);
            EditorNotificationPanel panel = new EditorNotificationPanel();

            if (isMainCumulative(project, file)){
                // В главном кумулятиве панель вообще не нужна
                return null;
            }

            if (!usages.isEmpty()) {
                String text = Localization.message("file.usages", usages.size());
                if (usages.size() == 1){
                    text += " - " + Utils.getDisplayPathCutProject(project, usages.get(0).getContainingFile().getVirtualFile().getPath());
                }

                // Добавляем кнопку перехода к первому найденному месту использования
                JLabel goToUsageButton = new JBLabel(text, UIUtil.ComponentStyle.LARGE, UIUtil.FontColor.BRIGHTER);
                goToUsageButton.setForeground(JBUI.CurrentTheme.Link.Foreground.ENABLED);
                goToUsageButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                goToUsageButton.setIcon(AllIcons.Icons.Ide.NextStep);
                goToUsageButton.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        new GoToUsagePopup(project).showUsagesPopup(usages, goToUsageButton);
                    }
                });

                panel.add(goToUsageButton);
            }
            else {
                if (!StaticSettings.ENABLE_NOT_LOADED_NOTIFICATIONS){
                    return null;
                }

                panel.icon(AllIcons.Debugger.Db_obsolete);
                String panelText = isFileMustBeLoadedByCumulative(project, file) ? Localization.message("file.not-loaded.cumulative") : Localization.message("file.not-loaded.changeset");
                panel.setText(panelText);
            }

            return panel;
        };
    }

    public boolean isMainCumulative(Project project, VirtualFile file){
        return file.getPath().endsWith(StartProjectComponent.getArtifactId(project) + "-cumulative.xml");
    }

    public boolean isFileMustBeLoadedByCumulative(Project project, VirtualFile file){
        if (file.getFileType().getName().equals("XML")) {
            // Читаем файл как XML
            XmlFile xmlFile = Utils.parseXmlFile(project, file);
            if (xmlFile != null) {
                // Проверяем наличие тега <databaseChangeLog>
                XmlTag rootTag = xmlFile.getRootTag();
                if (rootTag != null){
                    // Если корневой тег или первый саб-тег равны databaseChangeLog, то этот файл загружается кумулятивом
                    if ("databaseChangeLog".equals(rootTag.getName())) return true;
                    XmlTag databaseChangeLogTag = rootTag.findFirstSubTag("databaseChangeLog");
                    return databaseChangeLogTag != null;
                }
            }
        }
        return false;
    }

}
