package by.alis.functionalservercontrol.spigot.managers.mute;

import org.bukkit.OfflinePlayer;

import static by.alis.functionalservercontrol.databases.DataBases.getSQLiteManager;
import static by.alis.functionalservercontrol.spigot.additional.containers.StaticContainers.getMutedPlayersContainer;
import static by.alis.functionalservercontrol.spigot.additional.globalsettings.StaticSettingsAccessor.getConfigSettings;

public class MuteChecker {

    /**
     * Checks if null player is muted
     * @param nullPlayerName - player name who never player on the server
     * @return true if nickname muted
     */
    public static boolean isPlayerMuted(String nullPlayerName) {
        if(getConfigSettings().isAllowedUseRamAsContainer()) {
            return getMutedPlayersContainer().getNameContainer().contains(nullPlayerName);
        } else {
            switch (getConfigSettings().getStorageType()) {
                case SQLITE: {
                    return getSQLiteManager().getMutedPlayersNames().contains(nullPlayerName);
                }
                
                case H2: {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Checks if a player is muted
     * @param player - player to be tested
     * @return true if player muted
     */
    public static boolean isPlayerMuted(OfflinePlayer player) {
        if(getConfigSettings().isAllowedUseRamAsContainer()) {
            return getMutedPlayersContainer().getNameContainer().contains(player.getName()) && getMutedPlayersContainer().getUUIDContainer().contains(String.valueOf(player.getUniqueId()));
        } else {
            switch (getConfigSettings().getStorageType()) {
                case SQLITE: {
                    return getSQLiteManager().getMutedUUIDs().contains(String.valueOf(player.getUniqueId())) && getSQLiteManager().getMutedPlayersNames().contains(player.getName());
                }
                
                case H2: {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Checks if ip is muted
     * @param ipAddress - ip to be tested
     * @return true if IP muted
     */
    public static boolean isIpMuted(String ipAddress) {
        if(getConfigSettings().isAllowedUseRamAsContainer()) {
            return getMutedPlayersContainer().getIpContainer().contains(ipAddress);
        } else {
            switch (getConfigSettings().getStorageType()) {
                case SQLITE: {
                    return getSQLiteManager().getMutedIps().contains(ipAddress);
                }
                case H2: {
                    break;
                }
            }
        }
        return false;
    }

    /**
     * Checks if the IP of the specified player is muted
     * @param player - player whose ip will be verified
     * @return true if player ip is muted
     */
    public static boolean isIpMuted(OfflinePlayer player) {

        if(getConfigSettings().isAllowedUseRamAsContainer()) {
            return getMutedPlayersContainer().getIpContainer().contains(getSQLiteManager().getIpByUUID(player.getUniqueId()));
        } else {
            switch (getConfigSettings().getStorageType()) {
                case SQLITE: {
                    return getSQLiteManager().getMutedIps().contains(getSQLiteManager().getIpByUUID(player.getUniqueId())) && getSQLiteManager().getMutedUUIDs().contains(String.valueOf(player.getUniqueId()));
                }
                
                case H2: {
                    return false;
                }
            }
        }
        return false;
    }

}
