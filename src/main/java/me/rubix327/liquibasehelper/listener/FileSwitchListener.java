package me.rubix327.liquibasehelper.listener;

import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotifications;
import org.jetbrains.annotations.NotNull;

public class FileSwitchListener implements FileEditorManagerListener {
    private final Project project;

    public FileSwitchListener(Project project) {
        this.project = project;
    }

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        VirtualFile file = event.getNewFile();
        if (file != null) {
            // Обновляет панель уведомлений при переходе в файл
            EditorNotifications.getInstance(project).updateNotifications(file);
        }
    }
}