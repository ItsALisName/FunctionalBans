package by.alis.functionalservercontrol.spigot.Commands.Completers;

import by.alis.functionalservercontrol.spigot.Additional.Misc.TextUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static by.alis.functionalservercontrol.spigot.Additional.Containers.StaticContainers.getMutedPlayersContainer;
import static by.alis.functionalservercontrol.spigot.Additional.GlobalSettings.StaticSettingsAccessor.getConfigSettings;

public class MuteListCompleter implements TabCompleter {

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(getConfigSettings().isAllowedUseRamAsContainer() && sender.hasPermission("functionalservercontrol.mutelist")) {
            int i = 1;
            List<String> a = new ArrayList<>();
            do {
                a.add(String.valueOf(i));
                i = i + 1;
            } while (i < (getMutedPlayersContainer().getIdsContainer().size() / 10));
            return TextUtils.sortList(a, args);
        }
        return Collections.singletonList("");
    }
}
