package cf.maybelambda.fedora;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class PostInstallUpdaterTests {
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

    @Test
    void helpOptionDisplaysHelpTextAndExits() {
        try (MockedStatic<ConfigManager> filesMock = mockStatic(ConfigManager.class)) {
            filesMock.when(() -> ConfigManager.readResourceLines(any(String.class)))
                .thenReturn(List.of("Usage instructions go here"));

            PostInstallUpdater.main(new String[]{"--help"});
            PostInstallUpdater.main(new String[]{"-h"});

            filesMock.verify(() -> ConfigManager.getHelpText(), Mockito.times(2));
        }
    }
}
