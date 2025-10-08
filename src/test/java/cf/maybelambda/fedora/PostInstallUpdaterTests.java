package cf.maybelambda.fedora;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class PostInstallUpdaterTests {

    @Test
    void confirmYesInput() {
        Scanner scanner = new Scanner(new ByteArrayInputStream("y\n".getBytes(StandardCharsets.UTF_8)));

        assertTrue(PostInstallUpdater.confirm(scanner, "Install packages?"));
    }

    @Test
    void confirmYesFullWord() {
        Scanner scanner = new Scanner(new ByteArrayInputStream("yes\n".getBytes(StandardCharsets.UTF_8)));

        assertTrue(PostInstallUpdater.confirm(scanner, "Confirm?"));
    }

    @Test
    void confirmNoInput() {
        Scanner scanner = new Scanner(new ByteArrayInputStream("no\n".getBytes(StandardCharsets.UTF_8)));

        assertFalse(PostInstallUpdater.confirm(scanner, "Confirm?"));
    }

    @Test
    void confirmEmptyDefaultsToNo() {
        Scanner scanner = new Scanner(new ByteArrayInputStream("\n".getBytes(StandardCharsets.UTF_8)));

        assertFalse(PostInstallUpdater.confirm(scanner, "Proceed?"));
    }

    @Test
    void promptForExclusionsKeepsAllWhenEmpty() {
        List<String> pkgs = List.of("nano", "vim", "htop");
        Scanner scanner = new Scanner(new ByteArrayInputStream("\n".getBytes(StandardCharsets.UTF_8)));

        List<String> result = PostInstallUpdater.promptForExclusions(pkgs, scanner);

        assertEquals(pkgs, result);
    }

    @Test
    void promptForExclusionsRemovesSome() {
        List<String> pkgs = List.of("nano", "vim", "htop", "curl");
        Scanner scanner = new Scanner(new ByteArrayInputStream("2,4\n".getBytes(StandardCharsets.UTF_8)));

        List<String> result = PostInstallUpdater.promptForExclusions(pkgs, scanner);

        assertEquals(List.of("nano", "htop"), result);
    }

    @Test
    void promptForExclusionsHandlesInvalidIndexes() {
        List<String> pkgs = List.of("pkg1", "pkg2", "pkg3");
        Scanner scanner = new Scanner(new ByteArrayInputStream("0,5,2\n".getBytes(StandardCharsets.UTF_8)));

        List<String> result = PostInstallUpdater.promptForExclusions(pkgs, scanner);

        assertEquals(List.of("pkg1", "pkg3"), result);
    }

    @Test
    void loadPackageNamesFromValidFileParsesPackageNamesAndOmitsComments() throws IOException {
        Path tempFile = Files.createTempFile("test-packages", ".cf");
        Files.writeString(tempFile, """
            # Comment
            nano
            vim

            # Another comment
            htop
        """, StandardCharsets.UTF_8);

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.readAllLines(any(Path.class), eq(StandardCharsets.UTF_8)))
                    .thenReturn(List.of("# Comment", "nano", "vim", "", "htop"));

            List<String> result = PostInstallUpdater.loadPackageNamesFrom("dummy.cf");

            assertEquals(List.of("nano", "vim", "htop"), result);
        }
    }

    @Test
    void loadPackageNamesFromReturnsEmptyListOnIOException() {
        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.readAllLines(any(Path.class), eq(StandardCharsets.UTF_8)))
                    .thenThrow(new IOException("Simulated failure"));

            List<String> result = PostInstallUpdater.loadPackageNamesFrom("missing.cf");

            assertTrue(result.isEmpty());
        }
    }

    @Test
    void runCommandReturnsStatusCodeOfCommandExecuted() throws Exception {
        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream("ok".getBytes(StandardCharsets.UTF_8)));
        when(mockProcess.waitFor()).thenReturn(0);

        ProcessBuilder mockBuilder = mock(ProcessBuilder.class);
        when(mockBuilder.start()).thenReturn(mockProcess);
        when(mockBuilder.redirectErrorStream(true)).thenReturn(mockBuilder);

        try (MockedStatic<PostInstallUpdater> updaterMock = Mockito.mockStatic(PostInstallUpdater.class, CALLS_REAL_METHODS)) {
            updaterMock.when(() -> PostInstallUpdater.createProcessBuilder(any(String[].class)))
                    .thenReturn(mockBuilder);

            int exitCode = PostInstallUpdater.runCommand(new String[]{"echo", "test"});

            assertEquals(0, exitCode);
        }
    }

    @Test
    void runCommandReturnsStatusMinusOneOnIOException() throws Exception {
        ProcessBuilder mockBuilder = mock(ProcessBuilder.class);
        when(mockBuilder.start()).thenThrow(new IOException("Simulated I/O error"));

        try (MockedStatic<PostInstallUpdater> updaterMock = Mockito.mockStatic(PostInstallUpdater.class, CALLS_REAL_METHODS)) {
            updaterMock.when(() -> PostInstallUpdater.createProcessBuilder(any(String[].class)))
                    .thenReturn(mockBuilder);

            int exitCode = PostInstallUpdater.runCommand(new String[]{"failing", "cmd"});

            assertEquals(-1, exitCode);
        }
    }

    @Test
    void runCommandSkipsExecutionInDryRunMode() {
        PostInstallUpdater.setDryRun(true);
        try (MockedStatic<PostInstallUpdater> updaterMock = Mockito.mockStatic(PostInstallUpdater.class, CALLS_REAL_METHODS)) {
            updaterMock.when(() -> PostInstallUpdater.createProcessBuilder(any(String[].class)))
                .thenThrow(new AssertionError("Should not create ProcessBuilder in dry-run mode"));

            int exitCode = PostInstallUpdater.runCommand(new String[]{"fake", "cmd"});

            assertEquals(0, exitCode);
        } finally {
            PostInstallUpdater.setDryRun(false);
        }
    }
}
