package me.rubix327.liquibasehelper.log;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

@SuppressWarnings("unused")
public class MainLogger {
    private static final Logger LOGGER = Logger.getLogger("LiquibaseHelperLogger");

    static {
        try {
            RotatingDateTimeFileHandler handler = new RotatingDateTimeFileHandler();
            handler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    return String.format(
                            "%1$tF %1$tT [%2$s] %3$s %n",
                            record.getMillis(),
                            record.getLevel().getName(),
                            record.getMessage());
                }
            });
            LOGGER.addHandler(handler);
            LOGGER.setUseParentHandlers(false);
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
        info(0, s, args);
    }

    public static void info(int offset, @NotNull String s, Object... args){
        String offsetStr = "  ".repeat(offset);
        String result = offsetStr + String.format(s, args);
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
