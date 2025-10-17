package cf.maybelambda.fedora;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

public class ConfigManagerTests {

    @Test
    void readResourceLinesReadsFromFilesystemWhenExists() throws IOException {
        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.exists(any(Path.class))).thenReturn(true);
            filesMock.when(() -> Files.readAllLines(any(Path.class), any(Charset.class))).thenReturn(List.of("line1", "line2"));

            List<String> result = ConfigManager.readResourceLines("testfile.txt");

            assertEquals(List.of("line1", "line2"), result);
        }
    }

    @Test
    void readResourceLinesReadsFromClasspathWhenFileNotExists() throws IOException {
        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            // Force the filesystem check to say the file is NOT present in CONFIG_DIR
            filesMock.when(() -> Files.exists(any(Path.class))).thenReturn(false);

            // Ensure the test resource "test.txt" is placed in src/test/resources
            List<String> result = ConfigManager.readResourceLines("test.txt");

            assertEquals(List.of("one", "two", "three"), result);

            filesMock.verify(() -> Files.exists(any(Path.class)));
        }
    }

    @Test
    void readResourceLinesThrowsIOExceptionWhenResourceMissing() {
        assertThrows(IOException.class, () -> ConfigManager.readResourceLines("missing.txt"));
    }

    @Test
    void loadPackageNamesFromValidFileParsesPackageNamesAndOmitsComments() {
        try (MockedStatic<ConfigManager> updaterMock = Mockito.mockStatic(ConfigManager.class, CALLS_REAL_METHODS)) {
            updaterMock.when(() -> ConfigManager.readResourceLines(any(String.class)))
                    .thenReturn(List.of("# Comment", "nano", "vim", "", "htop"));

            List<String> result = ConfigManager.loadPackageNamesFrom("test-packages.cf");

            assertEquals(List.of("nano", "vim", "htop"), result);
        }
    }

    @Test
    void loadPackageNamesFromReturnsEmptyListOnIOException() {
        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.readAllLines(any(Path.class), eq(StandardCharsets.UTF_8)))
                    .thenThrow(new IOException("Simulated failure"));

            List<String> result = ConfigManager.loadPackageNamesFrom("missing.cf");

            assertTrue(result.isEmpty());
        }
    }
}
