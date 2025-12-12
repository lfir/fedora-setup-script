package cf.maybelambda.fedora;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PostInstallUpdaterTests {
    private PostInstallUpdater updater;
    private ProcessBuilder mockBuilder;

    @BeforeEach
    void setUp() {
        this.updater = Mockito.spy(new PostInstallUpdater());
        this.mockBuilder = mock(ProcessBuilder.class);
    }

    @Test
    void createProcessBuilderReturnsProcessBuilderWithExpectedCommand() {
        String[] expectedCommand = {"ls", "-la"};
        ProcessBuilder mockBuilder = mock(ProcessBuilder.class);
        Mockito.doReturn(mockBuilder).when(updater).createProcessBuilder(expectedCommand);
        
        ProcessBuilder result = updater.createProcessBuilder(expectedCommand);
        
        assertEquals(mockBuilder, result);
        Mockito.verify(updater).createProcessBuilder(expectedCommand);
    }

    @Test
    void runCommandReturnsStatusCodeOfCommandExecuted() throws Exception {
        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream("ok".getBytes(StandardCharsets.UTF_8)));
        when(mockProcess.waitFor()).thenReturn(0);
        when(mockBuilder.start()).thenReturn(mockProcess);
        when(mockBuilder.redirectErrorStream(true)).thenReturn(mockBuilder);
        Mockito.doReturn(mockBuilder).when(updater).createProcessBuilder(any(String[].class));

        int exitCode = updater.runCommand(asList("echo"), asList("test"));

        assertEquals(0, exitCode);
    }

    @Test
    void runCommandReturnsStatusMinusOneOnIOException() throws Exception {
        when(mockBuilder.start()).thenThrow(new IOException("Simulated I/O error"));
        Mockito.doReturn(mockBuilder).when(updater).createProcessBuilder(any(String[].class));

        int exitCode = updater.runCommand(asList("failing"), asList("cmd"));

        assertEquals(-1, exitCode);
    }

    @Test
    void runCommandSkipsExecutionInDryRunMode() {
        updater.setDryRun(true);
        Mockito.doThrow(new AssertionError("Should not create ProcessBuilder in dry-run mode"))
            .when(updater).createProcessBuilder(any(String[].class));

        int exitCode = updater.runCommand(asList("fake"), asList("cmd"));

        assertEquals(0, exitCode);
    }
}
