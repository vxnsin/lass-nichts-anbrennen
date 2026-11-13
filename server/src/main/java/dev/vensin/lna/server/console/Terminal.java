package dev.vensin.lna.server.console;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

/** Must run before anything prints: it decides whether colors, symbols and the spinner are available. */
public final class Terminal {

    private static final int UTF_8_CODE_PAGE = 65001;
    private static final int STD_OUTPUT_HANDLE = -11;
    private static final int ENABLE_VIRTUAL_TERMINAL_PROCESSING = 0x0004;

    private static boolean interactive;

    private Terminal() {}

    public static void prepare() {
        interactive = System.console() != null && System.console().isTerminal();
        if (interactive && System.getProperty("os.name", "").startsWith("Windows")) {
            try {
                switchWindowsConsoleToUtf8();
                System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
                System.setProperty("stdout.encoding", "UTF-8");
            } catch (Throwable unsupported) {
                // the ASCII fallback in Ansi takes over
            }
        }
    }

    /** True when a person is watching, so animations make sense. */
    public static boolean interactive() {
        return interactive;
    }

    /** Windows consoles default to a legacy code page and to plain text; both are per-console switches in kernel32. */
    private static void switchWindowsConsoleToUtf8() throws Throwable {
        Linker linker = Linker.nativeLinker();
        SymbolLookup kernel = SymbolLookup.libraryLookup("kernel32", Arena.global());
        MethodHandle setCodePage = linker.downcallHandle(kernel.find("SetConsoleOutputCP").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        MethodHandle getHandle = linker.downcallHandle(kernel.find("GetStdHandle").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        MethodHandle getMode = linker.downcallHandle(kernel.find("GetConsoleMode").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        MethodHandle setMode = linker.downcallHandle(kernel.find("SetConsoleMode").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

        if ((int) setCodePage.invoke(UTF_8_CODE_PAGE) == 0) {
            throw new IllegalStateException("code page not switched");
        }
        MemorySegment console = (MemorySegment) getHandle.invoke(STD_OUTPUT_HANDLE);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment mode = arena.allocate(ValueLayout.JAVA_INT);
            if ((int) getMode.invoke(console, mode) != 0) {
                int unused = (int) setMode.invoke(console,
                        mode.get(ValueLayout.JAVA_INT, 0) | ENABLE_VIRTUAL_TERMINAL_PROCESSING);
            }
        }
    }
}
