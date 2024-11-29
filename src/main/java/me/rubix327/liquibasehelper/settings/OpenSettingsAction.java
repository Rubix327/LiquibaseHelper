package me.rubix327.liquibasehelper.settings;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class OpenSettingsAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        PluginSettingsDialog dialog = new PluginSettingsDialog(e.getProject());
        dialog.show();
    }

}
