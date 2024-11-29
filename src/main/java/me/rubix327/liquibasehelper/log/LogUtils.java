package me.rubix327.liquibasehelper.log;

import java.io.File;

public class LogUtils {
    public static File getLogFile() {
        String pluginDir = System.getProperty("user.home") + "/.liquibaseHelper/logs";
        File logDir = new File(pluginDir);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        return new File(logDir, "plugin.log");
    }
}
