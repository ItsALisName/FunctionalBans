package by.alis.functionalservercontrol.spigot.Listeners.Old;

import by.alis.functionalservercontrol.spigot.Managers.PlayerCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;

public class TabCompleteListener implements Listener {
    private final PlayerCommandManager commandManager = new PlayerCommandManager();
    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        if (event.getSender() instanceof Player) {
            event.setCompletions(this.commandManager.getNewCompletions(event.getSender(), event.getBuffer().split(" ")[0].toLowerCase(), event.getCompletions()));
        }
    }

}
