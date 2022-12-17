package by.alis.functionalbans.spigot.Additional.TimerTasks.Tasks;

import by.alis.functionalbans.spigot.Additional.Other.TemporaryCache;
import by.alis.functionalbans.spigot.FunctionalBansSpigot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static by.alis.functionalbans.spigot.Additional.GlobalSettings.StaticSettingsAccessor.getConfigSettings;

public class DupeIpTask extends BukkitRunnable {

    @Override
    public void run() {
        if(getConfigSettings().getDupeIpCheckMode().equalsIgnoreCase("timer")) {
            int count = 0;
            List<Player> dupeIpPlayers = new ArrayList<>();
            for(Player player : Bukkit.getOnlinePlayers()) {
                String playerIp = player.getAddress().getAddress().getHostAddress();
                for(Map.Entry<Player, String> e : TemporaryCache.getOnlineIps().entrySet()) {
                    if(e.getValue().equalsIgnoreCase(playerIp)) {
                        count = count + 1;
                        dupeIpPlayers.add(e.getKey());
                    }
                }

                if(count > getConfigSettings().getMaxIpsPerSession()) {
                    for(Player dupeIpPlayer : dupeIpPlayers) {
                        Bukkit.getScheduler().runTask(FunctionalBansSpigot.getProvidingPlugin(FunctionalBansSpigot.class), () -> {
                           Bukkit.dispatchCommand(Bukkit.getConsoleSender(), getConfigSettings().getDupeIpAction().replace("%1$f", dupeIpPlayer.getName()));
                        });
                    }
                }

                count = 0;
                dupeIpPlayers.clear();
            }
        }
    }

}
