package cf.maybelambda.fedora;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;

public class PostInstallUpdater {
    private static final List<String> gpgKeys = List.of(
        "https://rpmfusion.org/keys?action=AttachFile&do=get&target=RPM-GPG-KEY-rpmfusion-free-fedora-2020",
        "https://rpmfusion.org/keys?action=AttachFile&do=get&target=RPM-GPG-KEY-rpmfusion-nonfree-fedora-2020"
    );
    private static final List<String> rpmfusionRepos = List.of(
        "https://download1.rpmfusion.org/free/fedora/rpmfusion-free-release-42.noarch.rpm",
        "https://download1.rpmfusion.org/nonfree/fedora/rpmfusion-nonfree-release-42.noarch.rpm"
    );
    private static final String flatpakRemoteName = "flathub";
    private static final String flatpakRemoteUrl = "https://dl.flathub.org/repo/flathub.flatpakrepo";
    private static final List<String> groupList = List.of(
        "docker", "libvirt", "vboxsf", "vboxusers"
    );
    private static final String DNF_INSTALL_FILE = "dnf-install.cf";
    private static final String DNF_REMOVE_FILE = "dnf-remove.cf";
    private static final String FLATPAK_INSTALL_FILE = "flatpak-install.cf";

    private static final String RESET  = "\u001B[0m";
    private static final String YELLOW = "\u001B[33m";

    private static boolean dryRun = false;

    public static void main(String[] args) {
        setDryRun(Arrays.asList(args).contains("--dry-run"));
        List<String> dnfInstallPackages = loadPackageNamesFrom(DNF_INSTALL_FILE);
        List<String> dnfRemovePackages = loadPackageNamesFrom(DNF_REMOVE_FILE);
        List<String> flatpakInstallPackages = loadPackageNamesFrom(FLATPAK_INSTALL_FILE);
        Scanner scanner = new Scanner(System.in);

        System.out.println("Fedora Post Install Actions\n");
        if (isDryRun()) {
            System.out.println("---[Dry Run Mode] Commands will not be executed.---\n");
        }

        // 1. Install RPMFusion repos
        if (confirm(scanner, "Install RPMFusion repos?")) {
            for (String key : gpgKeys) {
                runCommand(new String[]{"sudo", "rpm", "--import", key});
            }

            String[] cmd = new String[rpmfusionRepos.size() + 4];
            cmd[0] = "sudo";
            cmd[1] = "dnf";
            cmd[2] = "install";
            cmd[3] = "-y";
            for (int i = 0; i < rpmfusionRepos.size(); i++) {
                cmd[4 + i] = rpmfusionRepos.get(i);
            }
            runCommand(cmd);
        }

        // 2. DNF install packages
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

        // 3. DNF remove packages
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

        // 4. Flatpak install apps
        if (confirm(scanner, "Install Flatpak apps?")) {
            runCommand(new String[]{"flatpak", "remote-add", "--if-not-exists", flatpakRemoteName, flatpakRemoteUrl});
            List<String> filtered = promptForExclusions(flatpakInstallPackages, scanner);
            String[] cmd = new String[filtered.size() + 4];
            cmd[0] = "flatpak";
            cmd[1] = "install";
            cmd[2] = "-y";
            cmd[3] = flatpakRemoteName;
            for (int i = 0; i < filtered.size(); i++) {
                cmd[4 + i] = filtered.get(i);
            }
            runCommand(cmd);
        }

        // 5. Ensure groups exist and add user to them
        if (confirm(scanner, "Ensure admin groups exist and add current user to them?")) {
            String user = System.getProperty("user.name");
            for (String group : groupList) {
                int exit = runCommand(new String[]{"getent", "group", group});
                boolean groupExists = (exit == 0);
                if (!groupExists) {
                    System.out.println("Group '" + group + "' does not exist. Creating...");
                    runCommand(new String[]{"sudo", "groupadd", group});
                }
                runCommand(new String[]{"sudo", "usermod", "-aG", group, user});
            }
        }

        // 6. Enable and start Cockpit service
        if (confirm(scanner, "Enable and start cockpit.socket service?")) {
            runCommand(new String[]{"sudo", "systemctl", "enable", "--now", "cockpit.socket"});
        }

        System.out.println("\nAll actions completed. Goodbye.");
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
        System.out.println("Executing shell command: " + String.join(" ", command));
        if (isDryRun()) {
            System.out.println("Dry-run: command not executed.");
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
                System.out.println(YELLOW + line + RESET);
            }
            exitCode = process.waitFor();
            System.out.println("Command exited with code: " + exitCode);
        } catch (IOException | InterruptedException | NoSuchElementException e) {
            System.err.println("Error while running command: " + e.getMessage());
        }

        return exitCode;
    }

    static List<String> loadPackageNamesFrom(String filename) {
        List<String> packages = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(Path.of("src/main/resources", filename), StandardCharsets.UTF_8);
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    packages.add(trimmed);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to read package list from " + filename + ": " + e.getMessage());
        }

        return packages;
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
}
