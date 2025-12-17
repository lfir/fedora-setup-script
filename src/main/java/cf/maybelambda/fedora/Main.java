package cf.maybelambda.fedora;

import static cf.maybelambda.fedora.ConsoleIOHelper.GREEN;
import static cf.maybelambda.fedora.ConsoleIOHelper.RED;
import static cf.maybelambda.fedora.ConsoleIOHelper.color;
import static cf.maybelambda.fedora.ConsoleIOHelper.confirm;
import static cf.maybelambda.fedora.ConsoleIOHelper.promptForExclusions;
import static java.util.Arrays.asList;

import java.util.List;
import java.util.Scanner;

public class Main {
    static List<String> CMD_RPM_IMPORT = asList("sudo", "rpm", "--import");
    static List<String> CMD_DNF_INST_REPOS = asList("sudo", "dnf", "install", "-y");
    static List<String> CMD_DNF_INST = asList("sudo", "dnf", "--refresh", "install", "-y");
    static List<String> CMD_DNF_RM = asList("sudo", "dnf", "remove", "-y", "--noautoremove");
    static List<String> CMD_DNF_MARK = asList("sudo", "dnf", "mark", "-y", "user", "flatpak"); // single arg appended to cmd
    static List<String> CMD_DNF_AUTORM = asList("sudo", "dnf", "autoremove", "-y");
    static List<String> CMD_FLATPAK_REMOTE_ADD = asList("sudo", "flatpak", "remote-add", "--if-not-exists");
    static List<String> CMD_FLATPAK_INST = asList("flatpak", "install", "-y");
    static List<String> CMD_GETENT = asList("getent", "group");
    static List<String> CMD_ADD_GROUP = asList("sudo", "groupadd");
    static List<String> CMD_ADD_USER_TO_GROUP = asList("sudo", "usermod", "-aG");
    static List<String> CMD_SYSTEMCTL_ENABLE = asList("sudo", "systemctl", "enable", "--now", "cockpit.socket"); // single arg appended to cmd

    /**
     * Entry point of the program.
     * Delegates execution to the {@code Main.run} method.
     */
    public static void main(String[] args) {
        run(args, new PostInstallUpdater());
    }

    /**
     * Executes the setup workflow based on provided command-line arguments.
     *
     * <p>This method parses command-line arguments and checks for known flags, sets up interactive prompts,
     * and orchestrates various system configuration steps.
     *
     * @param args Command-line arguments passed to the program at startup
     * @param updater {@link PostInstallUpdater} responsible for executing OS commands
     */
    static void run(String[] args, PostInstallUpdater updater) {
        if (asList(args).contains("-h") || asList(args).contains("--help")) {
            ConsoleIOHelper.printHelp();
            return;
        }

        System.out.println(color("]|I{•------» Fedora Setup Script «------•}I|[\n", GREEN));
        
        List<String> dnfInstallPackages = ConfigManager.getDnfInstallPackages();
        List<String> dnfRemovePackages = ConfigManager.getDnfRemovePackages();
        List<String> flatpakInstallPackages = ConfigManager.getFlatpakInstallPackages();
        Scanner scanner = new Scanner(System.in);
        
        updater.setDryRun(asList(args).contains("--dry-run"));
        if (updater.isDryRun()) {
            System.out.println(color("---[Dry Run Mode] Shell Commands will not be executed.---\n", RED));
        }

        if (confirm(scanner, "Install RPMFusion repos?")) {
            for (String key : ConfigManager.getRPMFusionGpgKeys()) {
                updater.runCommand(CMD_RPM_IMPORT, asList(key));
            }

            List<String> repos = ConfigManager.getRPMFusionRepos();
            updater.runCommand(CMD_DNF_INST_REPOS, repos);
        }

        if (confirm(scanner, "Install additional packages with DNF?")) {
            List<String> filtered = promptForExclusions(dnfInstallPackages, scanner);
            updater.runCommand(CMD_DNF_INST, filtered);
        }

        if (confirm(scanner, "Remove all DNF packages marked for removal?")) {
            List<String> filtered = promptForExclusions(dnfRemovePackages, scanner);
            updater.runCommand(CMD_DNF_RM, filtered);
            updater.runCommand(CMD_DNF_MARK, asList());
            updater.runCommand(CMD_DNF_AUTORM, asList());
        }

        if (confirm(scanner, "Install Flatpak apps?")) {
            String name = ConfigManager.getFlatpakRemoteName();
            String url = ConfigManager.getFlatpakRemoteUrl();
            updater.runCommand(CMD_FLATPAK_REMOTE_ADD, asList(name, url));

            List<String> filtered = promptForExclusions(flatpakInstallPackages, scanner);
            filtered.addFirst(name);
            updater.runCommand(CMD_FLATPAK_INST, filtered);
        }

        if (confirm(scanner, "Ensure admin groups exist and add current user to them?")) {
            String user = System.getProperty("user.name");
            for (String group : ConfigManager.getAdminGroups()) {
                int exit = updater.runCommand(CMD_GETENT, asList(group));
                boolean groupExists = (exit == 0);
                if (!groupExists) {
                    System.out.println("Group '" + group + "' does not exist. Creating...");
                    updater.runCommand(CMD_ADD_GROUP, asList(group));
                }
                updater.runCommand(CMD_ADD_USER_TO_GROUP, asList(group, user));
            }
        }

        if (confirm(scanner, "Enable and start cockpit.socket service?")) {
            updater.runCommand(CMD_SYSTEMCTL_ENABLE, asList());
        }

        System.out.println(color("\n.o0×X×0o. All actions completed. Goodbye. .o0×X×0o.", GREEN));
    }
}
