package me.rubix327.liquibasehelper.log;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {

    @Override
    public String format(LogRecord record) {
        return String.format(
                "%1$tF %1$tT [%2$s] %3$s %n",
                record.getMillis(),
                record.getLevel().getName(),
                record.getMessage()
        );
    }

}
