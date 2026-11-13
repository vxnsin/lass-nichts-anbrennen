package dev.vensin.lna.server.console;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.LayoutBase;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class LogLayout extends LayoutBase<ILoggingEvent> {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final String OWN_LOGGERS = "dev.vensin.lna";

    @Override
    public String doLayout(ILoggingEvent event) {
        StringBuilder line = new StringBuilder("  ")
                .append(Ansi.dim(TIME.format(Instant.ofEpochMilli(event.getTimeStamp()))))
                .append("  ");
        String message = event.getFormattedMessage();
        if (event.getLevel().isGreaterOrEqual(Level.ERROR)) {
            line.append(Ansi.red(Ansi.ERROR + " " + message));
        } else if (event.getLevel().isGreaterOrEqual(Level.WARN)) {
            line.append(Ansi.yellow(Ansi.WARN + " " + message));
        } else {
            line.append(message);
        }
        if (!event.getLoggerName().startsWith(OWN_LOGGERS)) {
            String name = event.getLoggerName();
            line.append(Ansi.dim("  [" + name.substring(name.lastIndexOf(".") + 1) + "]"));
        }
        line.append(System.lineSeparator());
        if (event.getThrowableProxy() != null) {
            for (String row : ThrowableProxyUtil.asString(event.getThrowableProxy()).lines().toList()) {
                line.append("            ").append(Ansi.dim(row.strip())).append(System.lineSeparator());
            }
        }
        return line.toString();
    }
}
