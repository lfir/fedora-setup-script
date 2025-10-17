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
}
