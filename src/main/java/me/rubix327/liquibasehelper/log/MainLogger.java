package me.rubix327.liquibasehelper.log;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

@SuppressWarnings("unused")
public class MainLogger {
    private static final Logger LOGGER = Logger.getLogger("LiquibaseHelperLogger");

    static {
        try {
            FileHandler fileHandler = new FileHandler(LogUtils.getLogFile().getAbsolutePath(), true);
            fileHandler.setFormatter(new LogFormatter());
            LOGGER.addHandler(fileHandler);
            LOGGER.setUseParentHandlers(false); // Чтобы избежать дублирования логов в консоли IDE
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void info(@NotNull Project project, int offset, @NotNull String s, Object... args){
        String offsetStr = "  ".repeat(offset);
        String result = String.format("[%s] ", project.getName()) + offsetStr + String.format(s, args);
        System.out.println(result);
        LOGGER.info(result);
    }

    public static void info(@NotNull Project project, @NotNull String s, Object... args){
        info(project, 0, s, args);
    }

    public static void info(@NotNull String s, Object... args){
        String result = String.format(s, args);
        System.out.println(result);
        LOGGER.info(result);
    }

    public static void warn(@NotNull Project project, @NotNull String s, Object... args){
        String result = String.format("[%s] ", project.getName()) + String.format(s, args);
        System.out.println(result);
        LOGGER.warning(result);
    }

    public static void warn(@NotNull String s, Object... args){
        String result = String.format(s, args);
        System.out.println(result);
        LOGGER.warning(result);
    }

    public static void error(@NotNull Project project, @NotNull String s, Object... args){
        String result = String.format("[%s] ", project.getName()) + String.format(s, args);
        System.out.println(result);
        LOGGER.severe(result);
    }

    public static void error(@NotNull String s, Object... args){
        String result = String.format(s, args);
        System.out.println(result);
        LOGGER.severe(result);
    }

}
