package me.rubix327.liquibasehelper.form.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class OpenActionsMenuAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        PluginActionsDialog dialog = new PluginActionsDialog(e.getProject());
        dialog.show();
    }

}
