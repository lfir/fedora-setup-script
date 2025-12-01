package cf.maybelambda.fedora;

import static cf.maybelambda.fedora.ConsoleIOHelper.BLUE;
import static cf.maybelambda.fedora.ConsoleIOHelper.YELLOW;
import static cf.maybelambda.fedora.ConsoleIOHelper.color;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.NoSuchElementException;

public class PostInstallUpdater {
    private boolean dryRun;

    boolean isDryRun() {
        return dryRun;
    }

    void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    ProcessBuilder createProcessBuilder(String[] cmd) {
        return new ProcessBuilder(cmd);
    }

    int runCommand(String[] command) {
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
}
