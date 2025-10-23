package cf.maybelambda.fedora;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;

public class PostInstallUpdater {
    static final String RESET  = "\u001B[0m";
    static final String YELLOW = "\u001B[33m";
    static final String GREEN  = "\u001B[32m";
    private static final String RED    = "\u001B[31m";
    private static final String BLUE   = "\u001B[34m";

    private static boolean dryRun = false;

    public static void main(String[] args) {
        if (Arrays.asList(args).contains("-h") || Arrays.asList(args).contains("--help")) {
            printHelp();
            return;
        }

        List<String> dnfInstallPackages = ConfigManager.getDnfInstallPackages();
        List<String> dnfRemovePackages = ConfigManager.getDnfRemovePackages();
        List<String> flatpakInstallPackages = ConfigManager.getFlatpakInstallPackages();
        Scanner scanner = new Scanner(System.in);

        System.out.println(color("Fedora Post Install Actions\n", GREEN));
        setDryRun(Arrays.asList(args).contains("--dry-run"));
        if (isDryRun()) {
            System.out.println(color("---[Dry Run Mode] Shell Commands will not be executed.---\n", RED));
        }

        if (confirm(scanner, "Install RPMFusion repos?")) {
            for (String key : ConfigManager.getRPMFusionGpgKeys()) {
                runCommand(new String[]{"sudo", "rpm", "--import", key});
            }

            List<String> repos = ConfigManager.getRPMFusionRepos();
            String[] cmd = new String[repos.size() + 4];
            cmd[0] = "sudo";
            cmd[1] = "dnf";
            cmd[2] = "install";
            cmd[3] = "-y";
            for (int i = 0; i < repos.size(); i++) {
                cmd[4 + i] = repos.get(i);
            }
            runCommand(cmd);
        }

        if (confirm(scanner, "Install additional packages with DNF?")) {
            List<String> filtered = promptForExclusions(dnfInstallPackages, scanner);
            String[] cmd = new String[filtered.size() + 5];
            cmd[0] = "sudo";
            cmd[1] = "dnf";
            cmd[2] = "--refresh";
            cmd[3] = "install";
            cmd[4] = "-y";
            for (int i = 0; i < filtered.size(); i++) {
                cmd[5 + i] = filtered.get(i);
            }
            runCommand(cmd);
        }

        if (confirm(scanner, "Remove all DNF packages marked for removal?")) {
            List<String> filtered = promptForExclusions(dnfRemovePackages, scanner);
            String[] cmd = new String[filtered.size() + 4];
            cmd[0] = "sudo";
            cmd[1] = "dnf";
            cmd[2] = "remove";
            cmd[3] = "-y";
            for (int i = 0; i < filtered.size(); i++) {
                cmd[4 + i] = filtered.get(i);
            }
            runCommand(cmd);
            runCommand(new String[]{"sudo", "dnf", "autoremove", "-y"});
        }

        if (confirm(scanner, "Install Flatpak apps?")) {
            String name = ConfigManager.getFlatpakRemoteName();
            String url = ConfigManager.getFlatpakRemoteUrl();
            runCommand(new String[]{"flatpak", "remote-add", "--if-not-exists", name, url});
            List<String> filtered = promptForExclusions(flatpakInstallPackages, scanner);
            String[] cmd = new String[filtered.size() + 4];
            cmd[0] = "flatpak";
            cmd[1] = "install";
            cmd[2] = "-y";
            cmd[3] = name;
            for (int i = 0; i < filtered.size(); i++) {
                cmd[4 + i] = filtered.get(i);
            }
            runCommand(cmd);
        }

        if (confirm(scanner, "Ensure admin groups exist and add current user to them?")) {
            String user = System.getProperty("user.name");
            for (String group : ConfigManager.getAdminGroups()) {
                int exit = runCommand(new String[]{"getent", "group", group});
                boolean groupExists = (exit == 0);
                if (!groupExists) {
                    System.out.println("Group '" + group + "' does not exist. Creating...");
                    runCommand(new String[]{"sudo", "groupadd", group});
                }
                runCommand(new String[]{"sudo", "usermod", "-aG", group, user});
            }
        }

        if (confirm(scanner, "Enable and start cockpit.socket service?")) {
            runCommand(new String[]{"sudo", "systemctl", "enable", "--now", "cockpit.socket"});
        }

        System.out.println(color("\nAll actions completed. Goodbye.", GREEN));
    }

    static boolean isDryRun() {
        return dryRun;
    }

    static void setDryRun(boolean dryRun) {
        PostInstallUpdater.dryRun = dryRun;
    }

    static boolean confirm(Scanner scanner, String prompt) {
        System.out.print(prompt + " [y/N]: ");
        String input = scanner.nextLine().trim().toLowerCase();
        return input.equals("y") || input.equals("yes");
    }

    static ProcessBuilder createProcessBuilder(String[] cmd) {
        return new ProcessBuilder(cmd);
    }

    static int runCommand(String[] command) {
        System.out.println("Executing shell command: " + color(String.join(" ", command), BLUE));
        if (isDryRun()) {
            System.out.println(color("Dry-run: command not executed.", YELLOW));
            return 0;
        }

        int exitCode = -1;
        try {
            ProcessBuilder pb = createProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            System.out.println("Command output:");
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(color(line, YELLOW));
            }
            exitCode = process.waitFor();
            System.out.println("Command exited with code: " + exitCode);
        } catch (IOException | InterruptedException | NoSuchElementException e) {
            System.err.println("Error while running command: " + e.getMessage());
        }

        return exitCode;
    }

    static List<String> promptForExclusions(List<String> packages, Scanner scanner) {
        for (int i = 0; i < packages.size(); i++) {
            System.out.printf("%2d. %s\n", i + 1, packages.get(i));
        }
        System.out.print("Enter the numbers of any packages to exclude (comma-separated), or press Enter to keep all: ");

        String excludeInput = scanner.nextLine().trim();
        Set<Integer> excludeIndexes = new HashSet<>();
        if (excludeInput.matches("^\\d+$|^(\\d+,)+\\d$")) {
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
        return console != null && term != null && !"dumb".equals(term);
    }
}
