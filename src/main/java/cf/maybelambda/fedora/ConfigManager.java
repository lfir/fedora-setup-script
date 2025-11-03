package cf.maybelambda.fedora;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    private static final String CONFIG_DIR = "src/main/resources";
    static final String DNF_INSTALL_FILE = "dnf-install.cf";
    static final String DNF_REMOVE_FILE = "dnf-remove.cf";
    static final String FLATPAK_INSTALL_FILE = "flatpak-install.cf";
    static final String HELP_FILE = "help.txt";

    private static final List<String> gpgKeys = List.of(
        "https://rpmfusion.org/keys?action=AttachFile&do=get&target=RPM-GPG-KEY-rpmfusion-free-fedora-2020",
        "https://rpmfusion.org/keys?action=AttachFile&do=get&target=RPM-GPG-KEY-rpmfusion-nonfree-fedora-2020"
    );
    private static final List<String> rpmFusionRepos = List.of(
        "https://download1.rpmfusion.org/free/fedora/rpmfusion-free-release-42.noarch.rpm",
        "https://download1.rpmfusion.org/nonfree/fedora/rpmfusion-nonfree-release-42.noarch.rpm"
    );

    private static final String flatpakRemoteName = "flathub";
    private static final String flatpakRemoteUrl = "https://dl.flathub.org/repo/flathub.flatpakrepo";

    private static final List<String> adminGroups = List.of("docker", "libvirt", "vboxsf", "vboxusers");

    static List<String> readResourceLines(String filename) throws IOException {
        Path path = Path.of(CONFIG_DIR, filename);
        if (Files.exists(path)) {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        }
        try (var in = PostInstallUpdater.class.getResourceAsStream("/" + filename)) {
            if (in == null) throw new IOException("Resource not found: " + filename);
            return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines().toList();
        }
    }

    static List<String> loadPackageNamesFrom(String filename) {
        List<String> packages = new ArrayList<>();
        try {
            List<String> lines = readResourceLines(filename);
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

    static List<String> getRPMFusionGpgKeys() {
        return gpgKeys;
    }

    static List<String> getRPMFusionRepos() {
        return rpmFusionRepos;
    }

    static String getFlatpakRemoteName() {
        return flatpakRemoteName;
    }

    static String getFlatpakRemoteUrl() {
        return flatpakRemoteUrl;
    }

    static List<String> getAdminGroups() {
        return adminGroups;
    }

    static List<String> getDnfInstallPackages() {
        return loadPackageNamesFrom(DNF_INSTALL_FILE);
    }

    static List<String> getDnfRemovePackages() {
        return loadPackageNamesFrom(DNF_REMOVE_FILE);
    }

    static List<String> getFlatpakInstallPackages() {
        return loadPackageNamesFrom(FLATPAK_INSTALL_FILE);
    }

    static List<String> getHelpText() throws IOException {
        return readResourceLines(HELP_FILE);
    }
}
