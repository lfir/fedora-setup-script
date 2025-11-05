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

    static boolean confirm(Scanner scanner, String prompt) {
        System.out.print(prompt + " [y/N]: ");
        String input = scanner.nextLine().trim().toLowerCase();
        return input.equals("y") || input.equals("yes");
    }

    static List<String> promptForExclusions(List<String> packages, Scanner scanner) {
        for (int i = 0; i < packages.size(); i++) {
            System.out.printf("%2d. %s\n", i + 1, packages.get(i));
        }
        System.out.print("Enter the numbers of any packages to exclude (comma-separated), or press Enter to keep all: ");

        String excludeInput = scanner.nextLine().trim();
        String validInputRE = "^$|^\\d+$|^(\\d+,)+\\d$";
        if (!excludeInput.matches(validInputRE)) {
            throw new RuntimeException("Invalid selection");
        }

        Set<Integer> excludeIndexes = new HashSet<>();
        if (!excludeInput.isEmpty()) {
            for (String part : excludeInput.split(",")) {
                try {
                    int idx = Integer.parseInt(part.trim());
                    if (idx >= 1 && idx <= packages.size()) {
                        excludeIndexes.add(idx - 1);
                    }
                } catch (NumberFormatException ignored) {}
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

    static void printHelp() {
        try {
            List<String> lines = ConfigManager.getHelpText();
            System.out.println(String.join(System.lineSeparator(), lines));
        } catch (IOException e) {
            System.err.println("Error reading help file: " + e.getMessage());
        }
    }

    static String color(String str, String ansiColorCode) {
        return isANSISupported(System.getenv("TERM"), System.console()) ? ansiColorCode + str + RESET : str;
    }

    static boolean isANSISupported(String term, Console console) {
        return console != null && term != null && !term.isEmpty() && !"dumb".equals(term);
    }
}
