package me.rubix327.liquibasehelper.log;

import com.intellij.openapi.project.Project;

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

    public static void info(Project project, int offset, String s, Object... args){
        String offsetStr = "  ".repeat(offset);
        String result = String.format("[%s] ", project.getName()) + offsetStr + String.format(s, args);
        System.out.println(result);
        LOGGER.info(result);
    }

    public static void info(Project project, String s, Object... args){
        info(project, 0, s, args);
    }

    public static void info(String s, Object... args){
        String result = String.format(s, args);
        System.out.println(result);
        LOGGER.info(result);
    }

    public static void warn(Project project, String s, Object... args){
        String result = String.format("[%s] ", project.getName()) + String.format(s, args);
        System.out.println(result);
        LOGGER.warning(result);
    }

    public static void warn(String s, Object... args){
        String result = String.format(s, args);
        System.out.println(result);
        LOGGER.warning(result);
    }

    public static void error(Project project, String s, Object... args){
        String result = String.format("[%s] ", project.getName()) + String.format(s, args);
        System.out.println(result);
        LOGGER.severe(result);
    }

    public static void error(String s, Object... args){
        String result = String.format(s, args);
        System.out.println(result);
        LOGGER.severe(result);
    }

}
