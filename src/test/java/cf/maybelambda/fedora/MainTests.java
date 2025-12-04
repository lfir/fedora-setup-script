package cf.maybelambda.fedora;

import static cf.maybelambda.fedora.ConfigManager.getAdminGroups;
import static cf.maybelambda.fedora.ConfigManager.getDnfInstallPackages;
import static cf.maybelambda.fedora.ConfigManager.getDnfRemovePackages;
import static cf.maybelambda.fedora.ConfigManager.getFlatpakInstallPackages;
import static cf.maybelambda.fedora.ConfigManager.getFlatpakRemoteName;
import static cf.maybelambda.fedora.ConfigManager.getFlatpakRemoteUrl;
import static cf.maybelambda.fedora.ConfigManager.getRPMFusionGpgKeys;
import static cf.maybelambda.fedora.ConfigManager.getRPMFusionRepos;
import static cf.maybelambda.fedora.Main.CMD_ADD_USER_TO_GROUP;
import static cf.maybelambda.fedora.Main.CMD_DNF_AUTORM;
import static cf.maybelambda.fedora.Main.CMD_DNF_INST;
import static cf.maybelambda.fedora.Main.CMD_DNF_INST_REPOS;
import static cf.maybelambda.fedora.Main.CMD_DNF_MARK;
import static cf.maybelambda.fedora.Main.CMD_DNF_RM;
import static cf.maybelambda.fedora.Main.CMD_FLATPAK_INST;
import static cf.maybelambda.fedora.Main.CMD_FLATPAK_REMOTE_ADD;
import static cf.maybelambda.fedora.Main.CMD_GETENT;
import static cf.maybelambda.fedora.Main.CMD_RPM_IMPORT;
import static cf.maybelambda.fedora.Main.CMD_SYSTEMCTL_ENABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void runExecutesCommandStructureAndSequenceInCorrectOrdering() {
        try (MockedStatic<ConfigManager> cfg = mockStatic(ConfigManager.class)) {
            setupConfigManager(cfg);
            simulateUserInput();
            when(mockUpdater.runCommand(any(List.class), any(List.class))).thenReturn(0);

            Main.run(new String[]{}, mockUpdater);

            ArgumentCaptor<List<String>> captorPrefix = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<List<String>> captorArgs = ArgumentCaptor.forClass(List.class);
            Mockito.verify(mockUpdater, Mockito.atLeastOnce()).runCommand(captorPrefix.capture(), captorArgs.capture());
            List<List<String>> prefixes = captorPrefix.getAllValues();
            List<List<String>> args = captorArgs.getAllValues();
            int i = 0;

            assertEquals(CMD_RPM_IMPORT, prefixes.get(i));
            assertEquals(getRPMFusionGpgKeys(), args.get(i)); 
            i++;
            assertEquals(CMD_DNF_INST_REPOS, prefixes.get(i));
            assertEquals(getRPMFusionRepos(), args.get(i)); 
            i++;
            assertEquals(CMD_DNF_INST, prefixes.get(i)); 
            assertEquals(getDnfInstallPackages(), args.get(i)); 
            i++;
            assertEquals(CMD_DNF_RM, prefixes.get(i)); 
            assertEquals(getDnfRemovePackages(), args.get(i)); 
            i++;
            assertEquals(CMD_DNF_MARK, prefixes.get(i)); 
            assertTrue(args.get(i).isEmpty()); 
            i++;
            assertEquals(CMD_DNF_AUTORM, prefixes.get(i));
            assertTrue(args.get(i).isEmpty()); 
            i++;
            assertEquals(CMD_FLATPAK_REMOTE_ADD, prefixes.get(i));
            assertEquals(List.of(getFlatpakRemoteName(), getFlatpakRemoteUrl()), args.get(i)); 
            i++;
            assertEquals(CMD_FLATPAK_INST, prefixes.get(i));
            assertEquals(List.of(getFlatpakRemoteName(), getFlatpakInstallPackages().getFirst()), args.get(i)); 
            i++;
            assertEquals(CMD_GETENT, prefixes.get(i)); 
            assertEquals(getAdminGroups(), args.get(i)); 
            i++;
            assertEquals(CMD_ADD_USER_TO_GROUP, prefixes.get(i)); 
            assertTrue(args.get(i).containsAll(getAdminGroups())); 
            i++;
            assertEquals(CMD_SYSTEMCTL_ENABLE, prefixes.get(i)); 
            assertTrue(args.get(i).isEmpty());
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
