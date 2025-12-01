package cf.maybelambda.fedora;

import static cf.maybelambda.fedora.ConsoleIOHelper.GREEN;
import static cf.maybelambda.fedora.ConsoleIOHelper.RED;
import static cf.maybelambda.fedora.ConsoleIOHelper.color;
import static cf.maybelambda.fedora.ConsoleIOHelper.confirm;
import static cf.maybelambda.fedora.ConsoleIOHelper.promptForExclusions;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        if (Arrays.asList(args).contains("-h") || Arrays.asList(args).contains("--help")) {
            ConsoleIOHelper.printHelp();
            return;
        }

        System.out.println(color("]|I{•------» Fedora Setup Script «------•}I|[\n", GREEN));
        
        List<String> dnfInstallPackages = ConfigManager.getDnfInstallPackages();
        List<String> dnfRemovePackages = ConfigManager.getDnfRemovePackages();
        List<String> flatpakInstallPackages = ConfigManager.getFlatpakInstallPackages();
        Scanner scanner = new Scanner(System.in);
        PostInstallUpdater updater = new PostInstallUpdater();
        
        updater.setDryRun(Arrays.asList(args).contains("--dry-run"));
        if (updater.isDryRun()) {
            System.out.println(color("---[Dry Run Mode] Shell Commands will not be executed.---\n", RED));
        }

        if (confirm(scanner, "Install RPMFusion repos?")) {
            for (String key : ConfigManager.getRPMFusionGpgKeys()) {
                updater.runCommand(new String[]{"sudo", "rpm", "--import", key});
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
            updater.runCommand(cmd);
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
            updater.runCommand(cmd);
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
            updater.runCommand(cmd);
            updater.runCommand(new String[]{"sudo", "dnf", "autoremove", "-y"});
        }

        if (confirm(scanner, "Install Flatpak apps?")) {
            String name = ConfigManager.getFlatpakRemoteName();
            String url = ConfigManager.getFlatpakRemoteUrl();
            updater.runCommand(new String[]{"sudo", "flatpak", "remote-add", "--if-not-exists", name, url});
            List<String> filtered = promptForExclusions(flatpakInstallPackages, scanner);
            String[] cmd = new String[filtered.size() + 4];
            cmd[0] = "flatpak";
            cmd[1] = "install";
            cmd[2] = "-y";
            cmd[3] = name;
            for (int i = 0; i < filtered.size(); i++) {
                cmd[4 + i] = filtered.get(i);
            }
            updater.runCommand(cmd);
        }

        if (confirm(scanner, "Ensure admin groups exist and add current user to them?")) {
            String user = System.getProperty("user.name");
            for (String group : ConfigManager.getAdminGroups()) {
                int exit = updater.runCommand(new String[]{"getent", "group", group});
                boolean groupExists = (exit == 0);
                if (!groupExists) {
                    System.out.println("Group '" + group + "' does not exist. Creating...");
                    updater.runCommand(new String[]{"sudo", "groupadd", group});
                }
                updater.runCommand(new String[]{"sudo", "usermod", "-aG", group, user});
            }
        }

        if (confirm(scanner, "Enable and start cockpit.socket service?")) {
            updater.runCommand(new String[]{"sudo", "systemctl", "enable", "--now", "cockpit.socket"});
        }

        System.out.println(color("\n.o0×X×0o. All actions completed. Goodbye. .o0×X×0o.", GREEN));
    }
}
