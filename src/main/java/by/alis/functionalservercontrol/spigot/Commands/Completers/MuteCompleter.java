package by.alis.functionalservercontrol.spigot.Commands.Completers;

import by.alis.functionalservercontrol.spigot.Additional.SomeUtils.TemporaryCache;
import by.alis.functionalservercontrol.spigot.Additional.SomeUtils.TextUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MuteCompleter implements TabCompleter {

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
            if (args.length == 1) {
                List<String> a = new ArrayList<>();
                if (sender.hasPermission("functionalservercontrol.mute")) {
                    a.addAll(TemporaryCache.getOnlinePlayerNames());
                    a.addAll(TemporaryCache.getOnlineIps().values());
                    return TextUtils.sortList(a, args);
                }
                return Collections.singletonList("");
            }

            if (args[0].equalsIgnoreCase("-a") && args.length == 2) {
                if (sender.hasPermission("functionalservercontrol.mute") && sender.hasPermission("functionalservercontrol.use.unsafe-flags")) {
                    return Collections.singletonList("-s");
                }
                return Collections.singletonList("");
            }

            if (args[0].equalsIgnoreCase("-s") && args.length == 2) {
                List<String> a = new ArrayList<>();
                if (sender.hasPermission("functionalservercontrol.mute") && sender.hasPermission("functionalservercontrol.use.silently")) {
                    a.addAll(TemporaryCache.getOnlinePlayerNames());
                    a.addAll(TemporaryCache.getOnlineIps().values());
                }
                return TextUtils.sortList(a, args);
            }

            if (args[0].equalsIgnoreCase("-s") && args.length == 3) {
                List<String> a = new ArrayList<>();
                if (sender.hasPermission("functionalservercontrol.mute") && sender.hasPermission("functionalservercontrol.use.silently")) {
                    a.add("1min");
                    a.add("1day");
                    a.add("1mon");
                    return TextUtils.sortList(a, args);
                }
                return Collections.singletonList("");
            }

        return Collections.singletonList("");
    }

}