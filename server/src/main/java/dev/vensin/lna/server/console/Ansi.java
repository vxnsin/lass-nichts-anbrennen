package dev.vensin.lna.server.console;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/** NO_COLOR switches colors off; a console that is not UTF-8 gets ASCII symbols. */
public final class Ansi {

    public static final boolean COLOR = System.getenv("NO_COLOR") == null;
    public static final boolean UNICODE = StandardCharsets.UTF_8.equals(consoleCharset());

    public static final String OK = UNICODE ? "√" : "+";
    public static final String WARN = "!";
    public static final String ERROR = UNICODE ? "×" : "x";
    public static final String DOT = UNICODE ? "·" : "-";
    public static final String JOIN = UNICODE ? "→" : ">";
    public static final String LEAVE = UNICODE ? "←" : "<";
    public static final String PROMPT = UNICODE ? "›" : ">";

    public static final String CLEAR_LINE = "\r\u001b[2K";
    private static final String ESCAPE = "\u001b[";
    private static final String LINK = "\u001b]8;;";
    private static final String BELL = "\u0007";

    private Ansi() {}

    public static String color(int color256, String text) {
        return paint("38;5;" + color256, text);
    }

    public static String orange(String text) {
        return color(208, text);
    }

    public static String yellow(String text) {
        return color(220, text);
    }

    public static String green(String text) {
        return color(78, text);
    }

    public static String red(String text) {
        return color(203, text);
    }

    public static String dim(String text) {
        return color(245, text);
    }

    public static String bold(String text) {
        return paint("1", text);
    }

    /** One character cell holding two stacked pixels; a color of 0 is transparent. */
    public static String pixels(int top, int bottom) {
        if (top == 0 && bottom == 0) {
            return " ";
        }
        if (!COLOR) {
            return "#";
        }
        if (bottom == 0) {
            return ESCAPE + "38;5;" + top + "m▀" + ESCAPE + "0m";
        }
        if (top == 0) {
            return ESCAPE + "38;5;" + bottom + "m▄" + ESCAPE + "0m";
        }
        return ESCAPE + "38;5;" + top + ";48;5;" + bottom + "m▀" + ESCAPE + "0m";
    }

    /** A clickable link in terminals that support OSC 8; others just show the text. */
    public static String link(String url, String text) {
        return COLOR ? LINK + url + BELL + text + LINK + BELL : text;
    }

    private static String paint(String code, String text) {
        return COLOR ? ESCAPE + code + "m" + text + ESCAPE + "0m" : text;
    }

    private static Charset consoleCharset() {
        String name = System.getProperty("stdout.encoding");
        try {
            return name == null ? Charset.defaultCharset() : Charset.forName(name);
        } catch (IllegalArgumentException e) {
            return StandardCharsets.US_ASCII;
        }
    }
}
