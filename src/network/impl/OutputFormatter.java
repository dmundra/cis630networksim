package network.impl;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

class OutputFormatter extends Formatter {
    private static final String FORMAT =
        "%ta, %<td-%<tb-%<tY %<tH:%<tM:%<tS.%<tL %<tz [Thread #%4x] %6s %s: %s\n";
    
    public String format(LogRecord record) {
        return String.format(FORMAT,
                record.getMillis(), record.getThreadID(),
                record.getLevel(), record.getLoggerName(),
                formatMessage(record));
    }
}
