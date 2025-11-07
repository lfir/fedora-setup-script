package cf.maybelambda.fedora;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

import static cf.maybelambda.fedora.ConsoleIOHelper.GREEN;
import static cf.maybelambda.fedora.ConsoleIOHelper.RESET;
import static cf.maybelambda.fedora.ConsoleIOHelper.YELLOW;
import static cf.maybelambda.fedora.ConsoleIOHelper.color;
import static cf.maybelambda.fedora.ConsoleIOHelper.confirm;
import static cf.maybelambda.fedora.ConsoleIOHelper.isANSISupported;
import static cf.maybelambda.fedora.ConsoleIOHelper.printHelp;
import static cf.maybelambda.fedora.ConsoleIOHelper.promptForExclusions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class ConsoleIOHelperTests {
    @Test
    void confirmYesInput() {
        Scanner scanner = new Scanner(new ByteArrayInputStream("y\n".getBytes(StandardCharsets.UTF_8)));

        assertTrue(confirm(scanner, "Install packages?"));
    }

    @Test
    void confirmYesFullWord() {
        Scanner scanner = new Scanner(new ByteArrayInputStream("yes\n".getBytes(StandardCharsets.UTF_8)));

        assertTrue(confirm(scanner, "Confirm?"));
    }

    @Test
    void confirmNoInput() {
        Scanner scanner = new Scanner(new ByteArrayInputStream("no\n".getBytes(StandardCharsets.UTF_8)));

        assertFalse(confirm(scanner, "Confirm?"));
    }

    @Test
    void confirmEmptyDefaultsToNo() {
        Scanner scanner = new Scanner(new ByteArrayInputStream("\n".getBytes(StandardCharsets.UTF_8)));

        assertFalse(confirm(scanner, "Proceed?"));
    }

    @Test
    void promptForExclusionsKeepsAllWhenInputIsEmpty() {
        List<String> pkgs = List.of("nano", "vim", "htop");
        Scanner scanner = new Scanner(new ByteArrayInputStream("\n".getBytes(StandardCharsets.UTF_8)));

        List<String> result = promptForExclusions(pkgs, scanner);

        assertEquals(pkgs, result);
    }

    @Test
    void promptForExclusionsRemovesSome() {
        List<String> pkgs = List.of("nano", "vim", "htop", "curl");
        Scanner scanner = mock(Scanner.class);
        when(scanner.nextLine()).thenReturn("2,4\n");

        List<String> result = promptForExclusions(pkgs, scanner);

        assertEquals(List.of("nano", "htop"), result);

        when(scanner.nextLine()).thenReturn("4\n");
        result = promptForExclusions(pkgs, scanner);
        assertEquals(List.of("nano", "vim", "htop"), result);

        when(scanner.nextLine()).thenReturn("1,3..4\n");
        result = promptForExclusions(pkgs, scanner);
        assertEquals(List.of("vim"), result);

        when(scanner.nextLine()).thenReturn("1..3\n");
        result = promptForExclusions(pkgs, scanner);
        assertEquals(List.of("curl"), result);

        when(scanner.nextLine()).thenReturn("3..4,2\n");
        result = promptForExclusions(pkgs, scanner);
        assertEquals(List.of("nano"), result);
    }

    @Test
    void promptForExclusionsHandlesInvalidIndexes() {
        List<String> pkgs = List.of("pkg1", "pkg2", "pkg3");
        Scanner scanner = mock(Scanner.class);
        when(scanner.nextLine()).thenReturn("0,5,2\n");

        List<String> result = promptForExclusions(pkgs, scanner);

        assertEquals(List.of("pkg1", "pkg3"), result);

        when(scanner.nextLine()).thenReturn("0..1\n");
        result = promptForExclusions(pkgs, scanner);
        assertEquals(pkgs, result);

        when(scanner.nextLine()).thenReturn("1..99\n");
        result = promptForExclusions(pkgs, scanner);
        assertEquals(pkgs, result);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "1,,3",                     // multiple consecutive commas
        "1,3,",                     // trailing comma
        ",1,3",                     // leading comma
        "1a,2", "3, 5",             // invalid characters mixed with valid numbers
        "1,abc,2",                  // non-numeric characters
        "qwerty",                   // completely non-numeric input
        "!@#, $%^, &*()",           // special characters
        "1.54", "1...54", "6.-.12"  // malformed range
    })
    void promptForExclusionsThrowsRuntimeExceptionWhenInvalidInputRead(String input) {
        List<String> pkgs = List.of("pkg");
        Scanner scanner = mock(Scanner.class);
        when(scanner.nextLine()).thenReturn(input);

        assertThrows(RuntimeException.class, () -> promptForExclusions(pkgs, scanner));
    }

    @Test
    void colorAppliesAnsiWhenisANSISupportedTrue() {
        try (MockedStatic<ConsoleIOHelper> mocked = mockStatic(ConsoleIOHelper.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> isANSISupported(any(), any())).thenReturn(true);

            String result = color("Hello", YELLOW);

            assertEquals(YELLOW + "Hello" + RESET, result);
        }
    }

    @Test
    void colorReturnsPlainTextWhenisANSISupportedFalse() {
        try (MockedStatic<ConsoleIOHelper> mocked = mockStatic(ConsoleIOHelper.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> isANSISupported(any(), any())).thenReturn(false);

            String result = color("World", GREEN);

            assertEquals("World", result);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "xterm-256color",
        "xterm",
        "screen",
    })
    void isANSISupportedReturnsTrueWhenConsolePresentAndTermValid(String term) {
        assertTrue(isANSISupported(term, mock(Console.class)));
    }

    @Test
    void isANSISupportedReturnsFalseWhenTermOrConsoleInvalid() {
        assertFalse(isANSISupported("", mock(Console.class)));
        assertFalse(isANSISupported("dumb", mock(Console.class)));
        assertFalse(isANSISupported("xterm", null));
        assertFalse(isANSISupported(null, mock(Console.class)));
        assertFalse(isANSISupported(null, null));
    }

    @Test
    void printHelpDisplaysHelpTextSuccessfully() {
        try (MockedStatic<ConfigManager> mockedConfig = mockStatic(ConfigManager.class)) {
            List<String> helpLines = List.of("Help line 1", "Help line 2", "Help line 3");
            mockedConfig.when(ConfigManager::getHelpText).thenReturn(helpLines);
            // Capture System.out output
            ByteArrayOutputStream capture = new ByteArrayOutputStream();
            System.setOut(new PrintStream(capture));

            printHelp();

            // Reset System.out
            System.setOut(System.out);
            String output = capture.toString();

            assertTrue(output.contains(helpLines.get(0)));
            assertTrue(output.contains(helpLines.get(1)));
            assertTrue(output.contains(helpLines.get(2)));
        }
    }

    @Test
    void printHelpPrintsErrorMessageOnIOException() {
        try (MockedStatic<ConfigManager> mockedConfig = mockStatic(ConfigManager.class)) {
            mockedConfig.when(ConfigManager::getHelpText).thenThrow(new IOException("File not found"));
            // Capture System.err output
            ByteArrayOutputStream capture = new ByteArrayOutputStream();
            System.setErr(new PrintStream(capture));

            printHelp();

            // Reset System.err
            System.setErr(System.err);
            String errorOutput = capture.toString();

            assertTrue(errorOutput.contains("Error reading help file"));
            assertTrue(errorOutput.contains("File not found"));
        }
    }
}
