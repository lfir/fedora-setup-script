package cf.maybelambda.fedora;

import static cf.maybelambda.fedora.ConsoleIOHelper.BLUE;
import static cf.maybelambda.fedora.ConsoleIOHelper.YELLOW;
import static cf.maybelambda.fedora.ConsoleIOHelper.color;
import static java.util.stream.Stream.concat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.NoSuchElementException;

public class PostInstallUpdater {
    private boolean dryRun;

    boolean isDryRun() {
        return dryRun;
    }

    void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    /**
     * Creates a {@link ProcessBuilder} configured with the given command array.
     * 
     * @param cmd An array containing the executable command parts
     * @return New {@code ProcessBuilder} instance initialized with {@code cmd}
     *
     */
    ProcessBuilder createProcessBuilder(String[] cmd) {
        return new ProcessBuilder(cmd);
    }

    /**  
     * Executes a shell command built from {@code baseCmd} followed by {@code args}.  
     * 
     * <p>Prints the full command, displays each line of output,
     * waits for termination, reports the exit status, and returns that status code.
     * In dry‑run mode, it only informs the caller that no execution will occur.
     *
     * @param baseCmd List containing the initial command tokens  
     * @param args Additional arguments to append to {@code baseCmd}  
     * @return Exit code of the executed process, or {@code -1} if execution was not performed
     *         due to an error, or {@code 0} if dry-run was enabled
     */
    int runCommand(List<String> baseCmd, List<String> args) {
        String[] command = concat(baseCmd.stream(), args.stream()).toArray(String[]::new);
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
