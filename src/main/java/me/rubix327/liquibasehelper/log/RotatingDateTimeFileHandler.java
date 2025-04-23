package me.rubix327.liquibasehelper.log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class RotatingDateTimeFileHandler extends Handler {

    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String FILE_PREFIX = "plugin_";
    private static final String FILE_SUFFIX = ".log";

    private final File logDir;
    private OutputStreamWriter writer;
    private File currentFile;

    public RotatingDateTimeFileHandler() throws IOException {
        logDir = new File(System.getProperty("user.home") + "/.liquibaseHelper/logs");
        if (!logDir.exists()) logDir.mkdirs();
        openOrReuseLatestFile();
    }

    private void openOrReuseLatestFile() throws IOException {
        File latest = findLatestLogFile();
        if (latest != null && latest.length() < MAX_SIZE) {
            currentFile = latest;
        } else {
            currentFile = createNewLogFile();
        }
        writer = new OutputStreamWriter(new FileOutputStream(currentFile, true), StandardCharsets.UTF_8);
    }

    private File findLatestLogFile() {
        File[] logFiles = logDir.listFiles((dir, name) -> name.startsWith(FILE_PREFIX) && name.endsWith(FILE_SUFFIX));
        if (logFiles == null || logFiles.length == 0) return null;

        return Arrays.stream(logFiles)
                .max(Comparator.comparing(File::getName))
                .orElse(null);
    }

    private File createNewLogFile() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        return new File(logDir, FILE_PREFIX + timestamp + FILE_SUFFIX);
    }

    private void rotateIfNeeded() throws IOException {
        if (currentFile.length() >= MAX_SIZE) {
            writer.flush();
            writer.close();
            currentFile = createNewLogFile();
            writer = new OutputStreamWriter(new FileOutputStream(currentFile, true), StandardCharsets.UTF_8);
        }
    }

    @Override
    public synchronized void publish(LogRecord record) {
        try {
            if (!isLoggable(record)) return;
            rotateIfNeeded();
            String message = getFormatter().format(record);
            writer.write(message);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void flush() {
        try {
            if (writer != null) writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws SecurityException {
        try {
            if (writer != null) writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


