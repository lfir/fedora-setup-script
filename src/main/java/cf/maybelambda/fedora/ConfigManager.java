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

    /**
     * Reads all lines of a resource / configuration file.
     *
     * <p>The method first attempts to locate the file on the real filesystem under
     * {@code CONFIG_DIR}/{@code filename}. If that file exists, its contents are read.
     * When the file is not found in the configuration directory, the method falls back
     * to loading it from the current working directory, or finally from the
     * class‑path (e.g. {@code /filename}).
     *
     * <p>The resulting list contains each line of the resource exactly as it appears,
     * without any trailing line‑separator characters.
     *
     * @param filename The name of the file to read
     * @return List of lines from the requested resource
     * @throws IOException If an I/O error occurs while reading the file, 
     *                     or if the resource cannot be located on either the filesystem
     *                     nor the class‑path
     */
    static List<String> readResourceLines(String filename) throws IOException {
        Path path = Path.of(CONFIG_DIR, filename);
        if (Files.exists(path)) {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        }
        path = Path.of(filename);
        if (Files.exists(path)) {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        }
        try (var in = PostInstallUpdater.class.getResourceAsStream("/" + filename)) {
            if (in == null) throw new IOException("Resource not found: " + filename);
            return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines().toList();
        }
    }

    /**
     * Loads package names from a configuration file.
     *
     * <p>Reads the resource identified by {@code filename} located under 
     * {@link #CONFIG_DIR CONFIG_DIR} or the class-path. 
     * Each non‑empty line that does not start with '#' is treated as a package name.
     * Lines beginning with '#' are considered comments and ignored. If the file cannot be
     * accessed, an error message is written to {@code System.err} and an empty list is returned.
     *
     * @param filename The resource file name (e.g., {@code dnf-install.cf})
     * @return List containing the package identifiers found in the
     *         file; may be empty if no valid entries are present or an I/O error occurs
     */
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
