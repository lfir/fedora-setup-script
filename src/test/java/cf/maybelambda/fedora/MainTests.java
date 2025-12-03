package cf.maybelambda.fedora;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class MainTests {
    private PostInstallUpdater mockUpdater = mock(PostInstallUpdater.class);
    private static final List<String> EXPECTED_COMMAND_PARTS = List.of(
        "rpm --import",
        "dnf install -y repo1",
        "dnf --refresh install -y pkg1",
        "dnf remove -y --noautoremove pkg2",
        "dnf mark user flatpak",
        "dnf autoremove",
        "flatpak remote-add",
        "flatpak install -y flathub flatpak1",
        "getent group wheel",
        "usermod -aG wheel",
        "systemctl enable --now cockpit.socket"
    );

    private void setupConfigManager(MockedStatic<ConfigManager> cfg) {
        cfg.when(ConfigManager::getDnfInstallPackages).thenReturn(List.of("pkg1"));
        cfg.when(ConfigManager::getDnfRemovePackages).thenReturn(List.of("pkg2"));
        cfg.when(ConfigManager::getFlatpakInstallPackages).thenReturn(List.of("flatpak1"));
        cfg.when(ConfigManager::getRPMFusionGpgKeys).thenReturn(List.of("key1"));
        cfg.when(ConfigManager::getRPMFusionRepos).thenReturn(List.of("repo1"));
        cfg.when(ConfigManager::getFlatpakRemoteName).thenReturn("flathub");
        cfg.when(ConfigManager::getFlatpakRemoteUrl).thenReturn("https://flathub");
        cfg.when(ConfigManager::getAdminGroups).thenReturn(List.of("wheel"));
    }

    private void simulateUserInput() {
        // Simulate user input: answers to 9 prompts (y/y/empty/y/empty/y/empty/y/y)
        String input = String.join("\n", "y", "y", "", "y", "", "y", "", "y", "y") + "\n";
        System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
    }

    private void assertCommandSequence(List<String> actualCommands) {
        for (int i = 0; i < EXPECTED_COMMAND_PARTS.size(); i++) {
            assertTrue(actualCommands.get(i).contains(EXPECTED_COMMAND_PARTS.get(i)), 
                "Command at index " + i + " should contain '" + EXPECTED_COMMAND_PARTS.get(i) + "'");
        }
    }

    @Test
    void runExecutesAllActionsInExpectedOrder() {
        try (var cfg = mockStatic(ConfigManager.class)) {
            setupConfigManager(cfg);
            simulateUserInput();
            when(mockUpdater.runCommand(any(String[].class))).thenReturn(0);
            
            Main.run(new String[]{}, mockUpdater);
            
            ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
            Mockito.verify(mockUpdater, Mockito.atLeastOnce()).runCommand(captor.capture());
            List<String> joined = captor.getAllValues().stream()
                .map(arr -> String.join(" ", arr))
                .toList();
            
            assertCommandSequence(joined);
        }
    }

    @Test
    void helpOptionDisplaysHelpTextAndExits() {
        try (var filesMock = mockStatic(ConfigManager.class)) {
            filesMock.when(() -> ConfigManager.readResourceLines(any(String.class)))
                .thenReturn(List.of("Usage instructions go here"));

            Main.main(new String[]{"--help"});
            Main.main(new String[]{"-h"});

            filesMock.verify(() -> ConfigManager.getHelpText(), Mockito.times(2));
        }
    }
}
