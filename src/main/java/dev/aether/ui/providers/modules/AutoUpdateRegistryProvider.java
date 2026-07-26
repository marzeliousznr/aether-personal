package dev.aether.ui;

import dev.aether.update.UpdateChecker;
import dev.aether.ui.settings.InfoSetting;
import dev.aether.ui.settings.ModulesTab;
import dev.aether.ui.settings.SettingGroup;

import java.util.List;

public final class AutoUpdateRegistryProvider extends AbstractMiningRegistryProvider {
    public AutoUpdateRegistryProvider() {
        super(1);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        SettingGroup info = SettingGroup.of(
                "Update Check",
                "Checks for new releases and notifies you — never installs automatically",
                () -> true,
                v -> {})
                .add(new InfoSetting("Status",
                        () -> "Update check runs once per session. "
                                + "This app does not auto-update.\n\n"
                                + "To update: run git fetch upstream, review the diff, "
                                + "then git merge upstream/main.\n\n"
                                + "Latest known version: "
                                + (UpdateChecker.getCachedLatestVersion() != null
                                        ? UpdateChecker.getCachedLatestVersion()
                                        : "(not yet checked)"))
                        .multiline());

        return MainGUIRegistry.subTab(
                "Update Check",
                "Checks for new releases and notifies you — never installs automatically",
                List.of(info));
    }
}
