package cf.maybelambda.fedora;

import java.io.Console;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class ConsoleIOHelper {
    static final String RESET  = "\u001B[0m";
    static final String YELLOW = "\u001B[33m";
    static final String GREEN  = "\u001B[32m";
    static final String RED    = "\u001B[31m";
    static final String BLUE   = "\u001B[34m";
    private static final String EXCLUDE_PROMPT = "Type in the number and/or range of numbers of packages to exclude "
        + "(comma-separated, i.e. 2,3,5..9)\nor press Enter to keep all: ";

    /**
     * Prompts the user for a yes/no confirmation based on the provided {@code prompt}.
     *
     * @param scanner {@link Scanner} used to read user input; must not be {@code null}
     * @param prompt The message displayed before the confirmation prompt
     * @return {@code true} if the user confirms with "y" or "yes" (case insensitive),
     *         {@code false} otherwise
     */
    static boolean confirm(Scanner scanner, String prompt) {
        System.out.print(prompt + " [y/N]: ");
        String input = scanner.nextLine().trim().toLowerCase();
        return input.equals("y") || input.equals("yes");
    }

    /**
     * Prompts the user to specify which packages should be excluded from further processing.
     *
     * <p>The method prints an enumerated list of available package names and then reads a line
     * containing comma‑separated indices, ranges (e.g., "2..4"), or both. It validates that the
     * input is empty or contains number/s and/or range/s.
     * A list containing only the non‑excluded items is returned.
     *
     * @param packages A list of package identifiers presented for selection; must not be {@code null}
     * @param scanner {@link Scanner} used to read user input; must not be {@code null}
     * @return List of packages that were not selected for exclusion
     * @throws RuntimeException If the user input does not conform to the expected format
     */
    static List<String> promptForExclusions(List<String> packages, Scanner scanner) {
        for (int i = 0; i < packages.size(); i++) {
            System.out.printf("%2d. %s\n", i + 1, packages.get(i));
        }
        System.out.print(EXCLUDE_PROMPT);

        String excludeInput = scanner.nextLine().trim();
        String validInputRE = "^$|^(\\d+|\\d+\\.\\.\\d+)(,(\\d+|\\d+\\.\\.\\d+))*$";
        if (!excludeInput.matches(validInputRE)) {
            throw new RuntimeException("Invalid selection");
        }

        Set<Integer> excludeIndexes = new HashSet<>();
        for (String part : excludeInput.split(",")) {
            if (excludeInput.isEmpty()) {
                break;
            }
            if (part.contains("..")) {
                String[] limits = part.split("\\.\\.");
                int s = Integer.parseInt(limits[0]);
                int e = Integer.parseInt(limits[1]);
                if (s >= 1 && e <= packages.size()) {
                    for (int i = s; i <= e; i++) {
                        excludeIndexes.add(i - 1);
                    }
                }
            } else {
                int idx = Integer.parseInt(part);
                if (idx >= 1 && idx <= packages.size()) {
                    excludeIndexes.add(idx - 1);
                }
            }
        }

        List<String> filtered = new ArrayList<>();
        for (int i = 0; i < packages.size(); i++) {
            if (!excludeIndexes.contains(i)) {
                filtered.add(packages.get(i));
            }
        }
        return filtered;
    }

    /**
     * Prints help documentation to standard output.
     *
     * <p>Retrieves help info from {@link ConfigManager#getHelpText()} and outputs it.
     * If an {@link IOException} occurs while reading the help file,
     * an error message is written to standard error.
     */
    static void printHelp() {
        try {
            List<String> lines = ConfigManager.getHelpText();
            System.out.println(String.join(System.lineSeparator(), lines));
        } catch (IOException e) {
            System.err.println("Error reading help file: " + e.getMessage());
        }
    }

    /**
     * Wraps a string in ANSI color codes when the underlying terminal supports it.
     *
     * <p>If ANSI escape sequences are supported for the given {@code term} and an active console is present,
     * the resulting string will be prefixed with {@code ansiColorCode} and suffixed with a reset code.
     * If coloring is not supported, the original string is returned unchanged.
     *
     * @param str The text to be colored; must not be {@code null}
     * @param ansiColorCode One of the predefined ANSI color codes (e.g., {@link #YELLOW});
     *                      may also be an empty string for no coloring
     * @return A possibly-colored version of {@code str}, or {@code str} itself if ANSI is unsupported
     */
    static String color(String str, String ansiColorCode) {
        return isANSISupported(System.getenv("TERM"), System.console()) ? ansiColorCode + str + RESET : str;
    }

    /**
     * Determines whether the current environment likely supports ANSI escape sequences.
     *
     * <p>Checks that the terminal type ({@code term}) is non‑null and not equal to "dumb",
     * and verifies that an active console stream exists. Returns {@code true} when these
     * conditions indicate that colored output can be rendered; otherwise returns {@code false}.
     *
     * @param term Value of the TERM environment variable
     * @param console Underlying console object from {@link Console}; may be {@code null}
     *                on some platforms
     * @return {@code true} if ANSI escape sequences are supported, {@code false} otherwise
     */
    static boolean isANSISupported(String term, Console console) {
        return console != null && term != null && !term.isEmpty() && !"dumb".equals(term);
    }
}
