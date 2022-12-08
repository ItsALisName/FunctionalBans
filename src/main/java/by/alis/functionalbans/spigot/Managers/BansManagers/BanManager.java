package by.alis.functionalbans.spigot.Managers.BansManagers;

import by.alis.functionalbans.API.Spigot.Events.AsyncBanPreprocessEvent;
import by.alis.functionalbans.spigot.Additional.Enums.BanType;
import by.alis.functionalbans.spigot.Additional.GlobalSettings.GlobalVariables;
import by.alis.functionalbans.spigot.Additional.WorldDate.WorldTimeAndDateClass;
import by.alis.functionalbans.spigot.FunctionalBansSpigot;
import by.alis.functionalbans.spigot.Managers.IdsManager;
import by.alis.functionalbans.spigot.Managers.FilesManagers.FileAccessor;
import by.alis.functionalbans.spigot.Managers.TimeManagers.TimeManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import static by.alis.functionalbans.spigot.Additional.Containers.StaticContainers.getBannedPlayersContainer;
import static by.alis.functionalbans.spigot.Additional.GlobalSettings.StaticSettingsAccessor.getConfigSettings;
import static by.alis.functionalbans.spigot.Additional.GlobalSettings.StaticSettingsAccessor.getGlobalVariables;
import static by.alis.functionalbans.spigot.Additional.Other.TextUtils.setColors;
import static by.alis.functionalbans.databases.StaticBases.getSQLiteManager;

public class BanManager {

    FileAccessor accessor = new FileAccessor();
    GlobalVariables variables = new GlobalVariables();
    IdsManager idsManager = new IdsManager();
    TimeManager timeManager = new TimeManager();

    BanContainerManager banContainerManager = new BanContainerManager();

    public boolean isPlayerBanned(String nullPlayerName) {
        if(getConfigSettings().isAllowedUseRamAsContainer()) {
            return getBannedPlayersContainer().getNameContainer().contains(nullPlayerName);
        } else {
            if(getConfigSettings().getStorageType().equalsIgnoreCase("sqlite")) {
                return getSQLiteManager().getBannedPlayersNames().contains(nullPlayerName);
            } else if(getConfigSettings().getStorageType().equalsIgnoreCase("h2")) {
                return false;
            } else if(getConfigSettings().getStorageType().equalsIgnoreCase("mysql")) {
                return false;
            } else {
                return getSQLiteManager().getBannedPlayersNames().contains(nullPlayerName);
            }
        }
    }

    public boolean isPlayerBanned(OfflinePlayer player) {
        if(getConfigSettings().isAllowedUseRamAsContainer()) {
            return getBannedPlayersContainer().getIpContainer().contains(getSQLiteManager().selectIpByUUID(player.getUniqueId())) || getBannedPlayersContainer().getUUIDContainer().contains(String.valueOf(player.getUniqueId()));
        } else {
            if(getConfigSettings().getStorageType().equalsIgnoreCase("sqlite")) {
                return getSQLiteManager().getBannedUUIDs().contains(String.valueOf(player.getUniqueId()));
            } else if(getConfigSettings().getStorageType().equalsIgnoreCase("mysql")) {
                return false;
            } else if(getConfigSettings().getStorageType().equalsIgnoreCase("h2")) {
                return false;
            } else {
                return false;
            }
        }
    }

    public boolean isIpBanned(String ipAddress) {
        if(getConfigSettings().isAllowedUseRamAsContainer()) {
            return getBannedPlayersContainer().getIpContainer().contains(ipAddress);
        } else {
            if(getConfigSettings().getStorageType().equalsIgnoreCase("sqlite")) {
                return getSQLiteManager().getBannedIps().contains(ipAddress);
            } else if(getConfigSettings().getStorageType().equalsIgnoreCase("mysql")) {
                return false;
            } else if(getConfigSettings().getStorageType().equalsIgnoreCase("h2")) {
                return false;
            } else {
                return false;
            }
        }
    }

    public boolean isIpBanned(OfflinePlayer player) {
        if(getConfigSettings().isAllowedUseRamAsContainer()) {
            return getBannedPlayersContainer().getIpContainer().contains(getSQLiteManager().selectIpByUUID(player.getUniqueId()));
        } else {
            if(getConfigSettings().getStorageType().equalsIgnoreCase("sqlite")) {
                return getSQLiteManager().getBannedIps().contains(getSQLiteManager().selectIpByUUID(player.getUniqueId()));
            } else if(getConfigSettings().getStorageType().equalsIgnoreCase("mysql")) {
                return false;
            } else if(getConfigSettings().getStorageType().equalsIgnoreCase("h2")) {
                return false;
            } else {
                return false;
            }
        }
    }

    public void preformBan(OfflinePlayer player, BanType type, String reason, CommandSender initiator, long time, boolean announceBan) {

        String realTime = WorldTimeAndDateClass.getTime();
        String realDate = WorldTimeAndDateClass.getDate();
        if(type == BanType.PERMANENT_IP || type == BanType.PERMANENT_NOT_IP) time = -1;
        if(reason == null || reason.equalsIgnoreCase("")) {
            reason = getGlobalVariables().getDefaultReason();
        }
        String convertedTime;
        if(time < 0) {
            convertedTime = getGlobalVariables().getVarUnknownTime();
        } else {
            convertedTime = this.timeManager.convertFromMillis(time - System.currentTimeMillis());
        }
        String id = idsManager.getId();
        AsyncBanPreprocessEvent banPlayerEvent = new AsyncBanPreprocessEvent(id, player, initiator, type, time, reason, realTime, realDate, getConfigSettings().isApiEnabled(), convertedTime);
        if(getConfigSettings().isApiEnabled()) {
            if(!getConfigSettings().isApiProtectedByPassword()) {
                Bukkit.getPluginManager().callEvent(banPlayerEvent);
            } else {
                if(banPlayerEvent.getApiPassword() != null && banPlayerEvent.getApiPassword().equalsIgnoreCase(this.accessor.getGeneralConfig().getString("plugin-settings.api.spigot.password.password"))) {
                    Bukkit.getPluginManager().callEvent(banPlayerEvent);
                }
            }
        }
        if(banPlayerEvent.isCancelled()) return;
        reason = banPlayerEvent.getReason().replace("'", "\"");
        String initiatorName = null;
        if(initiator instanceof Player) {
            initiatorName = ((Player)initiator).getPlayerListName();
        } else if(initiator instanceof ConsoleCommandSender) {
            initiatorName = getGlobalVariables().getConsoleVariableName();
        }


        if(type == BanType.PERMANENT_NOT_IP) {
            if (isPlayerBanned(player)) {
                if (initiator.hasPermission("functionalbans.use.re-ban")) {
                    if (getConfigSettings().isAllowedUseRamAsContainer()) {
                        switch (getConfigSettings().getStorageType()) {
                            case "sqlite": {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ingored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ingored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), -1);
                                break;
                            }
                            case "mysql": {
                                break;
                            }
                            case "h2": {
                                break;
                            }
                            default: {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ingored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ingored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), -1);
                                break;
                            }
                        }
                        try { getBanContainerManager().removeFromBanContainer("-u", String.valueOf(player.getUniqueId())); } catch (NullPointerException ingored) {}
                        try { getBanContainerManager().removeFromBanContainer("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId())); } catch (NullPointerException ignored) {}
                        getBanContainerManager().addToBanContainer(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, String.valueOf(player.getUniqueId()), -1L);
                        if(player.isOnline()) {
                            player.getPlayer().kickPlayer(setColors(String.join("\n", this.accessor.getLang().getStringList("ban-message-format")).replace("%1$f", String.valueOf(id)).replace("%2$f", reason).replace("%3$f", initiatorName).replace("%4$f", realDate + ", " + realTime).replace("%5$f", getGlobalVariables().getVariableNever())));                            }
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.last-ban-removed").replace("%1$f", player.getName())));
                        if(announceBan) {
                            Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.ban.broadcast-message").replace("%2$f", player.getName()).replace("%3$f", reason).replace("%1$f", initiatorName)));
                        }
                        return;
                    } else {
                        switch (getConfigSettings().getStorageType()) {
                            case "sqlite": {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ingored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ingored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), -1);
                                break;
                            }
                            case "mysql": {
                                break;
                            }
                            case "h2": {
                                break;
                            }
                            default: {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ingored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ingored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), -1);
                                break;
                            }
                        }
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.last-ban-removed").replace("%1$f", player.getName())));
                        if(player.isOnline()) {
                            player.getPlayer().kickPlayer(setColors(String.join("\n", this.accessor.getLang().getStringList("ban-message-format")).replace("%1$f", String.valueOf(id)).replace("%2$f", reason).replace("%3$f", initiatorName).replace("%4$f", realDate + ", " + realTime).replace("%5$f", getGlobalVariables().getVariableNever())));
                        }
                        if(announceBan) {
                            Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.ban.broadcast-message").replace("%2$f", player.getName()).replace("%3$f", reason).replace("%1$f", initiatorName)));
                        }
                    }
                } else {
                    initiator.sendMessage(setColors(this.accessor.getLang().getString("other.already-banned")));
                    return;
                }
            } else {

                if(getConfigSettings().isAllowedUseRamAsContainer()) {
                    switch (getConfigSettings().getStorageType()) {
                        case "sqlite": {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), -1);
                            break;
                        }
                        case "mysql": {
                            break;
                        }
                        case "h2": {
                            break;
                        }
                        default: {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), -1);
                            break;
                        }
                    }
                    getBanContainerManager().addToBanContainer(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, String.valueOf(player.getUniqueId()), -1L);
                    if(announceBan) {
                        Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.ban.broadcast-message").replace("%2$f", player.getName()).replace("%3$f", reason).replace("%1$f", initiatorName)));
                    }
                    if(player.isOnline()) {
                        String finalReason = reason;
                        String finalInitiatorName = initiatorName;
                        Bukkit.getScheduler().runTask(FunctionalBansSpigot.getProvidingPlugin(FunctionalBansSpigot.class), () -> {
                            player.getPlayer().kickPlayer(setColors(String.join("\n", this.accessor.getLang().getStringList("ban-message-format")).replace("%1$f", String.valueOf(id)).replace("%2$f", finalReason).replace("%3$f", finalInitiatorName).replace("%4$f", realDate + ", " + realTime).replace("%5$f", getGlobalVariables().getVariableNever())));
                        });
                    }
                } else {
                    switch (getConfigSettings().getStorageType()) {
                        case "sqlite": {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), -1);
                            break;
                        }
                        case "mysql": {
                            break;
                        }
                        case "h2": {
                            break;
                        }
                        default: {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), -1);
                            break;
                        }
                    }
                    getBanContainerManager().addToBanContainer(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, String.valueOf(player.getUniqueId()), -1L);
                    if(announceBan) {
                        Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.ban.broadcast-message").replace("%2$f", player.getName()).replace("%3$f", reason).replace("%1$f", initiatorName)));
                    }
                    if(player.isOnline()) {
                        String finalReason = reason;
                        String finalInitiatorName = initiatorName;
                        Bukkit.getScheduler().runTask(FunctionalBansSpigot.getProvidingPlugin(FunctionalBansSpigot.class), () -> {
                            player.getPlayer().kickPlayer(setColors(String.join("\n", this.accessor.getLang().getStringList("ban-message-format")).replace("%1$f", String.valueOf(id)).replace("%2$f", finalReason).replace("%3$f", finalInitiatorName).replace("%4$f", realDate + ", " + realTime).replace("%5$f", getGlobalVariables().getVariableNever())));
                        });
                    }
                }
            }
        }

        if(type == BanType.PERMANENT_IP) {
            if(isIpBanned(player)) {
                if(initiator.hasPermission("functionalbans.use.re-ban")) {
                    if (getConfigSettings().isAllowedUseRamAsContainer()) {
                        switch (getConfigSettings().getStorageType()) {
                            case "sqlite": {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), -1);
                                break;
                            }
                            case "mysql": {
                                break;
                            }
                            case "h2": {
                                break;
                            }
                            default: {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), -1);
                                break;
                            }
                        }
                        try { getBanContainerManager().removeFromBanContainer("-u", String.valueOf(player.getUniqueId())); } catch (NullPointerException ingored) {}
                        try { getBanContainerManager().removeFromBanContainer("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId())); } catch (NullPointerException ignored) {}
                        getBanContainerManager().addToBanContainer(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, String.valueOf(player.getUniqueId()), -1L);
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.last-ban-removed").replace("%1$f", player.getName())));
                        if(announceBan) {
                            Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.ban-ip.broadcast-message").replace("%1$f", initiatorName).replace("%2$f", player.getName()).replace("%3$f", reason)));
                        }
                        if(player.isOnline()) {
                            String finalReason = reason;
                            String finalInitiatorName = initiatorName;
                            Bukkit.getScheduler().runTask(FunctionalBansSpigot.getProvidingPlugin(FunctionalBansSpigot.class), () -> {
                                player.getPlayer().kickPlayer(setColors(String.join("\n", this.accessor.getLang().getStringList("ban-message-format")).replace("%1$f", String.valueOf(id)).replace("%2$f", finalReason).replace("%3$f", finalInitiatorName).replace("%4$f", realDate + ", " + realTime).replace("%5$f", getGlobalVariables().getVariableNever())));
                            });
                        }
                    } else {
                        switch (getConfigSettings().getStorageType()) {
                            case "sqlite": {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), -1);
                                break;
                            }
                            case "mysql": {
                                break;
                            }
                            case "h2": {
                                break;
                            }
                            default: {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), -1);
                                break;
                            }
                        }
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.last-ban-removed").replace("%1$f", player.getName())));
                        if(announceBan) {
                            Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.ban-ip.broadcast-message").replace("%1$f", initiatorName).replace("%2$f", player.getName()).replace("%3$f", reason)));
                        }
                        if(player.isOnline()) {
                            String finalReason = reason;
                            String finalInitiatorName = initiatorName;
                            Bukkit.getScheduler().runTask(FunctionalBansSpigot.getProvidingPlugin(FunctionalBansSpigot.class), () -> {
                                player.getPlayer().kickPlayer(setColors(String.join("\n", this.accessor.getLang().getStringList("ban-message-format")).replace("%1$f", String.valueOf(id)).replace("%2$f", finalReason).replace("%3$f", finalInitiatorName).replace("%4$f", realDate + ", " + realTime).replace("%5$f", getGlobalVariables().getVariableNever())));
                            });
                        }
                        return;
                    }
                } else {
                    initiator.sendMessage(setColors(this.accessor.getLang().getString("other.already-banned")));
                    return;
                }
            } else {
                if(getConfigSettings().isAllowedUseRamAsContainer()) {
                    switch (getConfigSettings().getStorageType()) {
                        case "sqlite": {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), -1);
                            break;
                        }
                        case "mysql": {
                            break;
                        }
                        case "h2": {
                            break;
                        }
                        default: {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), -1);
                            break;
                        }
                    }
                    getBanContainerManager().addToBanContainer(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, String.valueOf(player.getUniqueId()), -1L);
                    if(announceBan) {
                        Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.ban-ip.broadcast-message").replace("%2$f", player.getName()).replace("%3$f", reason).replace("%1$f", initiatorName)));
                    }
                    if(player.isOnline()) {
                        String finalReason = reason;
                        String finalInitiatorName = initiatorName;
                        Bukkit.getScheduler().runTask(FunctionalBansSpigot.getProvidingPlugin(FunctionalBansSpigot.class), () -> {
                            player.getPlayer().kickPlayer(setColors(String.join("\n", this.accessor.getLang().getStringList("ban-message-format")).replace("%1$f", String.valueOf(id)).replace("%2$f", finalReason).replace("%3$f", finalInitiatorName).replace("%4$f", realDate + ", " + realTime).replace("%5$f", getGlobalVariables().getVariableNever())));
                        });
                    }
                } else {
                    switch (getConfigSettings().getStorageType()) {
                        case "sqlite": {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), -1);
                            break;
                        }
                        case "mysql": {
                            break;
                        }
                        case "h2": {
                            break;
                        }
                        default: {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), -1);
                            break;
                        }
                    }
                    if(announceBan) {
                        Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.ban-ip.broadcast-message").replace("%2$f", player.getName()).replace("%3$f", reason).replace("%1$f", initiatorName)));
                    }
                    if(player.isOnline()) {
                        String finalReason = reason;
                        String finalInitiatorName = initiatorName;
                        Bukkit.getScheduler().runTask(FunctionalBansSpigot.getProvidingPlugin(FunctionalBansSpigot.class), () -> {
                            player.getPlayer().kickPlayer(setColors(String.join("\n", this.accessor.getLang().getStringList("ban-message-format")).replace("%1$f", String.valueOf(id)).replace("%2$f", finalReason).replace("%3$f", finalInitiatorName).replace("%4$f", realDate + ", " + realTime).replace("%5$f", getGlobalVariables().getVariableNever())));
                        });
                    }
                    return;
                }
            }
        }

        if(type == BanType.TIMED_NOT_IP) {
            if (isPlayerBanned(player)) {
                if (initiator.hasPermission("functionalbans.use.re-ban")) {
                    if (getConfigSettings().isAllowedUseRamAsContainer()) {
                        switch (getConfigSettings().getStorageType()) {
                            case "sqlite": {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                                break;
                            }
                            case "mysql": {
                                break;
                            }
                            case "h2": {
                                break;
                            }
                            default: {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                                break;
                            }
                        }
                        try { getBanContainerManager().removeFromBanContainer("-u", String.valueOf(player.getUniqueId())); } catch (NullPointerException ignored) {}
                        try { getBanContainerManager().removeFromBanContainer("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId())); } catch (NullPointerException ignored) {}
                        getBanContainerManager().addToBanContainer(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, String.valueOf(player.getUniqueId()), banPlayerEvent.getBanTime());
                        if(player.isOnline()) {
                            String finalInitiatorName = initiatorName;
                            String finalReason = reason;
                            Bukkit.getScheduler().runTask(FunctionalBansSpigot.getProvidingPlugin(FunctionalBansSpigot.class), () -> {
                                player.getPlayer().kickPlayer(setColors(String.join("\n", this.accessor.getLang().getStringList("temporary-ban-message-format")).replace("%1$f", String.valueOf(id)).replace("%2$f", finalReason).replace("%3$f", finalInitiatorName).replace("%4$f", realDate + ", " + realTime).replace("%5$f", this.timeManager.convertFromMillis(banPlayerEvent.getBanTime() - System.currentTimeMillis()))));
                            });
                        }
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.last-ban-removed").replace("%1$f", player.getName())));
                        if(announceBan) {
                            Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban.broadcast-message").replace("%2$f", player.getName()).replace("%3$f", this.timeManager.convertFromMillis(banPlayerEvent.getBanTime() - System.currentTimeMillis())).replace("%1$f", initiatorName).replace("%4$f", reason)));
                        }
                        return;
                    } else {
                        switch (getConfigSettings().getStorageType()) {
                            case "sqlite": {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                                break;
                            }
                            case "mysql": {
                                break;
                            }
                            case "h2": {
                                break;
                            }
                            default: {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                                break;
                            }
                        }
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.last-ban-removed").replace("%1$f", player.getName())));
                        if(player.isOnline()) {
                            String finalReason = reason;
                            String finalInitiatorName = initiatorName;
                            Bukkit.getScheduler().runTask(FunctionalBansSpigot.getProvidingPlugin(FunctionalBansSpigot.class), () -> {
                                player.getPlayer().kickPlayer(setColors(String.join("\n", this.accessor.getLang().getStringList("temporary-ban-message-format")).replace("%1$f", String.valueOf(id)).replace("%2$f", finalReason).replace("%3$f", finalInitiatorName).replace("%4$f", realDate + ", " + realTime).replace("%5$f", this.timeManager.convertFromMillis(banPlayerEvent.getBanTime() - System.currentTimeMillis()))));
                            });
                        }
                        if(announceBan) {
                            Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban.broadcast-message").replace("%2$f", player.getName()).replace("%3$f", this.timeManager.convertFromMillis(banPlayerEvent.getBanTime() - System.currentTimeMillis())).replace("%1$f", initiatorName).replace("%4$f", reason)));
                        }
                    }
                    return;
                } else {
                    initiator.sendMessage(setColors(this.accessor.getLang().getString("other.already-banned")));
                    return;
                }
            } else {

                if(getConfigSettings().isAllowedUseRamAsContainer()) {
                    switch (getConfigSettings().getStorageType()) {
                        case "sqlite": {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                            break;
                        }
                        case "mysql": {
                            break;
                        }
                        case "h2": {
                            break;
                        }
                        default: {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                            break;
                        }
                    }
                    getBanContainerManager().addToBanContainer(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, String.valueOf(player.getUniqueId()), banPlayerEvent.getBanTime());
                    if(announceBan) {
                        Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban.broadcast-message").replace("%2$f", player.getName()).replace("%3$f", this.timeManager.convertFromMillis(banPlayerEvent.getBanTime() - System.currentTimeMillis())).replace("%1$f", initiatorName).replace("%4$f", reason)));
                    }
                    if(player.isOnline()) {
                        String finalReason = reason;
                        String finalInitiatorName = initiatorName;
                        Bukkit.getScheduler().runTask(FunctionalBansSpigot.getProvidingPlugin(FunctionalBansSpigot.class), () -> {
                            player.getPlayer().kickPlayer(setColors(String.join("\n", this.accessor.getLang().getStringList("temporary-ban-message-format")).replace("%1$f", String.valueOf(id)).replace("%2$f", finalReason).replace("%3$f", finalInitiatorName).replace("%4$f", realDate + ", " + realTime).replace("%5$f", this.timeManager.convertFromMillis(banPlayerEvent.getBanTime() - System.currentTimeMillis()))));
                        });
                    }
                } else {
                    switch (getConfigSettings().getStorageType()) {
                        case "sqlite": {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                            break;
                        }
                        case "mysql": {
                            break;
                        }
                        case "h2": {
                            break;
                        }
                        default: {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                            break;
                        }
                    }
                    if(player.isOnline()) {
                        String finalReason = reason;
                        String finalInitiatorName = initiatorName;
                        Bukkit.getScheduler().runTask(FunctionalBansSpigot.getProvidingPlugin(FunctionalBansSpigot.class), () -> {
                            player.getPlayer().kickPlayer(setColors(String.join("\n", this.accessor.getLang().getStringList("temporary-ban-message-format")).replace("%1$f", String.valueOf(id)).replace("%2$f", finalReason).replace("%3$f", finalInitiatorName).replace("%4$f", realDate + ", " + realTime).replace("%5$f", this.timeManager.convertFromMillis(banPlayerEvent.getBanTime() - System.currentTimeMillis()))));
                        });
                    }
                    if(announceBan) {
                        Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban.broadcast-message").replace("%2$f", player.getName()).replace("%3$f", this.timeManager.convertFromMillis(banPlayerEvent.getBanTime() - System.currentTimeMillis())).replace("%1$f", initiatorName).replace("%4$f", reason)));
                    }
                    return;
                }
            }
        }

        if(type == BanType.TIMED_IP) {
            if(isIpBanned(player)) {
                if(initiator.hasPermission("functionalbans.use.re-ban")) {
                    if (getConfigSettings().isAllowedUseRamAsContainer()) {
                        switch (getConfigSettings().getStorageType()) {
                            case "sqlite": {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                                break;
                            }
                            case "mysql": {
                                break;
                            }
                            case "h2": {
                                break;
                            }
                            default: {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                                break;
                            }
                        }
                        try { getBanContainerManager().removeFromBanContainer("-u", String.valueOf(player.getUniqueId())); } catch (NullPointerException ignored) {}
                        try { getBanContainerManager().removeFromBanContainer("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId())); } catch (NullPointerException ignored) {}
                        getBanContainerManager().addToBanContainer(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, String.valueOf(player.getUniqueId()), banPlayerEvent.getBanTime());
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.last-ban-removed").replace("%1$f", player.getName())));
                        if(announceBan) {
                            Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban-ip.broadcast-message").replace("%1$f", initiatorName).replace("%2$f", player.getName()).replace("%3$f", this.timeManager.convertFromMillis(banPlayerEvent.getBanTime() - System.currentTimeMillis())).replace("%4$f", reason)));
                        }
                        if(player.isOnline()) {
                            String finalReason = reason;
                            String finalInitiatorName = initiatorName;
                            Bukkit.getScheduler().runTask(FunctionalBansSpigot.getProvidingPlugin(FunctionalBansSpigot.class), () -> {
                                player.getPlayer().kickPlayer(setColors(String.join("\n", this.accessor.getLang().getStringList("temporary-ban-ip-message-format")).replace("%1$f", id).replace("%2$f", finalReason).replace("%3$f", finalInitiatorName)).replace("%4$f", realDate + ", " + realTime).replace("%5$f", this.timeManager.convertFromMillis(banPlayerEvent.getBanTime() - System.currentTimeMillis())));
                            });
                        }
                    } else {
                        switch (getConfigSettings().getStorageType()) {
                            case "sqlite": {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ingored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                                break;
                            }
                            case "mysql": {
                                break;
                            }
                            case "h2": {
                                break;
                            }
                            default: {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ingored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                                break;
                            }
                        }
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.last-ban-removed").replace("%1$f", player.getName())));
                        if(announceBan) {
                            Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban-ip.broadcast-message").replace("%1$f", initiatorName).replace("%2$f", player.getName()).replace("%3$f", this.timeManager.convertFromMillis(banPlayerEvent.getBanTime() - System.currentTimeMillis())).replace("%4$f", reason)));
                        }
                        if(player.isOnline()) {
                            String finalReason = reason;
                            String finalInitiatorName = initiatorName;
                            Bukkit.getScheduler().runTask(FunctionalBansSpigot.getProvidingPlugin(FunctionalBansSpigot.class), () -> {
                                player.getPlayer().kickPlayer(setColors(String.join("\n", this.accessor.getLang().getStringList("ban-ip-message-format")).replace("%1$f", id).replace("%2$f", finalReason).replace("%3$f", finalInitiatorName)).replace("%4$f", realDate + ", " + realTime).replace("%5$f", this.timeManager.convertFromMillis(banPlayerEvent.getBanTime() - System.currentTimeMillis())));
                            });
                        }
                        return;
                    }
                } else {
                    initiator.sendMessage(setColors(this.accessor.getLang().getString("other.already-banned")));
                    return;
                }
            } else {
                if(getConfigSettings().isAllowedUseRamAsContainer()) {
                    switch (getConfigSettings().getStorageType()) {
                        case "sqlite": {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                            break;
                        }
                        case "mysql": {
                            break;
                        }
                        case "h2": {
                            break;
                        }
                        default: {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                            break;
                        }
                    }
                    getBanContainerManager().addToBanContainer(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, String.valueOf(player.getUniqueId()), banPlayerEvent.getBanTime());
                    if(announceBan) {
                        Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban-ip.broadcast-message").replace("%1$f", initiatorName).replace("%2$f", player.getName()).replace("%3$f", this.timeManager.convertFromMillis(banPlayerEvent.getBanTime() - System.currentTimeMillis())).replace("%4$f", reason)));
                    }
                    if(player.isOnline()) {
                        String finalInitiatorName = initiatorName;
                        String finalReason = reason;
                        Bukkit.getScheduler().runTask(FunctionalBansSpigot.getProvidingPlugin(FunctionalBansSpigot.class), () -> {
                            player.getPlayer().kickPlayer(setColors(String.join("\n", this.accessor.getLang().getStringList("temporary-ban-ip-message-format")).replace("%1$f", id).replace("%2$f", finalReason).replace("%3$f", finalInitiatorName)).replace("%4$f", realDate + ", " + realTime).replace("%5$f", this.timeManager.convertFromMillis(banPlayerEvent.getBanTime() - System.currentTimeMillis())));
                        });
                    }
                } else {
                    switch (getConfigSettings().getStorageType()) {
                        case "sqlite": {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                            break;
                        }
                        case "mysql": {
                            break;
                        }
                        case "h2": {
                            break;
                        }
                        default: {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiator.getName(), reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                            break;
                        }
                    }
                    if(announceBan) {
                        Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban-ip.broadcast-message").replace("%1$f", initiatorName).replace("%2$f", player.getName()).replace("%3$f", this.timeManager.convertFromMillis(banPlayerEvent.getBanTime() - System.currentTimeMillis())).replace("%4$f", reason)));
                    }
                    if(player.isOnline()) {
                        String finalInitiatorName = initiatorName;
                        String finalReason = reason;
                        Bukkit.getScheduler().runTask(FunctionalBansSpigot.getProvidingPlugin(FunctionalBansSpigot.class), () -> {
                            player.getPlayer().kickPlayer(setColors(String.join("\n", this.accessor.getLang().getStringList("temporary-ban-ip-message-format")).replace("%1$f", id).replace("%2$f", finalReason).replace("%3$f", finalInitiatorName)).replace("%4$f", realDate + ", " + realTime).replace("%5$f", this.timeManager.convertFromMillis(banPlayerEvent.getBanTime() - System.currentTimeMillis())));
                        });
                    }
                }
            }
        }

    }

    public void preformBan(String player, BanType type, String reason, CommandSender initiator, long time, boolean announceBan) {

        String realTime = WorldTimeAndDateClass.getTime();
        String realDate = WorldTimeAndDateClass.getDate();
        String convertedTime;
        if(time < 0) {
            convertedTime = getGlobalVariables().getVarUnknownTime();
        } else {
            convertedTime = this.timeManager.convertFromMillis(time - System.currentTimeMillis());
        }
        String id = idsManager.getId();
        if(reason == null || reason.equalsIgnoreCase("")) {
            reason = getGlobalVariables().getDefaultReason();
        }
        AsyncBanPreprocessEvent banPlayerEvent = new AsyncBanPreprocessEvent(id, player, initiator, type, time, reason, realTime, realDate, getConfigSettings().isApiEnabled(), convertedTime);
        reason = banPlayerEvent.getReason().replace("'", "\"");
        time = banPlayerEvent.getBanTime();
        if(type == BanType.PERMANENT_IP || type == BanType.PERMANENT_NOT_IP) time = -1;
        if(getConfigSettings().isApiEnabled()) {
            if(!getConfigSettings().isApiProtectedByPassword()) {
                Bukkit.getPluginManager().callEvent(banPlayerEvent);
            } else {
                if(banPlayerEvent.getApiPassword() != null && banPlayerEvent.getApiPassword().equalsIgnoreCase(this.accessor.getGeneralConfig().getString("plugin-settings.api.spigot.password.password"))) {
                    Bukkit.getPluginManager().callEvent(banPlayerEvent);
                }
            }
        }
        if(banPlayerEvent.isCancelled()) return;

        String initiatorName = null;
        if(initiator instanceof Player) {
            initiatorName = ((Player)initiator).getPlayerListName();
        } else if(initiator instanceof ConsoleCommandSender) {
            initiatorName = getGlobalVariables().getConsoleVariableName();
        }

        if(type == BanType.PERMANENT_NOT_IP) {
            if(isPlayerBanned(player)) {
                if(initiator.hasPermission("functionalbans.use.re-ban")) {
                    if(getConfigSettings().isAllowedUseRamAsContainer()) {
                        if (getConfigSettings().getStorageType().equalsIgnoreCase("sqlite")) {
                            getSQLiteManager().deleteFromNullBannedPlayers("-n", player);
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, time);
                        } else if (getConfigSettings().getStorageType().equalsIgnoreCase("mysql")) {
                            return;
                        } else if (getConfigSettings().getStorageType().equalsIgnoreCase("h2")) {
                            return;
                        } else {
                            getSQLiteManager().deleteFromNullBannedPlayers("-n", player);
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, time);
                        }
                        getBanContainerManager().removeFromBanContainer("-n", player);
                        getBanContainerManager().addToBanContainer(id, "NULL_PLAYER", player, initiator.getName(), reason, type, realDate, realTime, "NULL_PLAYER", time);
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.last-ban-removed").replace("%1$f", player)));
                        if(announceBan) {
                            Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.ban.broadcast-message").replace("%2$f", player).replace("%3$f", reason).replace("%1$f", initiatorName)));
                        }
                        return;
                    } else {
                        if(getConfigSettings().getStorageType().equalsIgnoreCase("sqlite")) {
                            getSQLiteManager().deleteFromNullBannedPlayers("-n", player);
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, time);
                        } else if(getConfigSettings().getStorageType().equalsIgnoreCase("mysql")) {
                            return;
                        } else if(getConfigSettings().getStorageType().equalsIgnoreCase("h2")) {
                            return;
                        } else {
                            getSQLiteManager().deleteFromNullBannedPlayers("-n", player);
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, time);
                        }
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.last-ban-removed").replace("%1$f", player)));
                        if(announceBan) {
                            Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.ban.broadcast-message").replace("%2$f", player).replace("%3$f", reason).replace("%1$f", initiatorName)));
                        }
                        return;
                    }
                } else {
                    initiator.sendMessage(setColors(this.accessor.getLang().getString("other.already-banned") ));
                    return;
                }
            } else {
                if(getConfigSettings().isAllowedUseRamAsContainer()) {
                    if (getConfigSettings().getStorageType().equalsIgnoreCase("sqlite")) {
                        getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, time);
                    } else if (getConfigSettings().getStorageType().equalsIgnoreCase("mysql")) {
                        return;
                    } else if (getConfigSettings().getStorageType().equalsIgnoreCase("h2")) {
                        return;
                    } else {
                        getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, banPlayerEvent.getBanTime());
                    }
                    getBanContainerManager().addToBanContainer(id, "NULL_PLAYER", player, initiator.getName(), reason, type, realDate, realTime, "NULL_PLAYER", banPlayerEvent.getBanTime());
                    initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                    if(announceBan) {
                        Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.ban.broadcast-message").replace("%2$f", player).replace("%3$f", reason).replace("%1$f", initiatorName)));
                    }
                    return;
                } else {
                    if (getConfigSettings().getStorageType().equalsIgnoreCase("sqlite")) {
                        getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, time);
                    } else if (getConfigSettings().getStorageType().equalsIgnoreCase("mysql")) {
                        return;
                    } else if (getConfigSettings().getStorageType().equalsIgnoreCase("h2")) {
                        return;
                    } else {
                        getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, banPlayerEvent.getBanTime());
                    }
                    initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                    if(announceBan) {
                        Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.ban.broadcast-message").replace("%2$f", player).replace("%3$f", reason).replace("%1$f", initiatorName)));
                    }
                    return;
                }
            }
        }

        if(type == BanType.PERMANENT_IP) {
            if(isPlayerBanned(player)) {
                if(initiator.hasPermission("functionalbans.use.re-ban")) {
                    if(getConfigSettings().isAllowedUseRamAsContainer()) {
                        if (getConfigSettings().getStorageType().equalsIgnoreCase("sqlite")) {
                            getSQLiteManager().deleteFromNullBannedPlayers("-n", player);
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, time);
                        } else if (getConfigSettings().getStorageType().equalsIgnoreCase("mysql")) {
                            return;
                        } else if (getConfigSettings().getStorageType().equalsIgnoreCase("h2")) {
                            return;
                        } else {
                            getSQLiteManager().deleteFromNullBannedPlayers("-n", player);
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, banPlayerEvent.getBanTime());
                        }
                        getBanContainerManager().removeFromBanContainer("-n", player);
                        getBanContainerManager().addToBanContainer(id, "NULL_PLAYER", player, initiator.getName(), reason, type, realDate, realTime, "NULL_PLAYER", banPlayerEvent.getBanTime());
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.last-ban-removed").replace("%1$f", player)));
                        if(announceBan) {
                            Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.ban-ip.broadcast-message").replace("%2$f", player).replace("%3$f", reason).replace("%1$f", initiatorName)));
                        }
                        return;
                    } else {
                        if(getConfigSettings().getStorageType().equalsIgnoreCase("sqlite")) {
                            getSQLiteManager().deleteFromNullBannedPlayers("-n", player);
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, time);
                        } else if(getConfigSettings().getStorageType().equalsIgnoreCase("mysql")) {
                            return;
                        } else if(getConfigSettings().getStorageType().equalsIgnoreCase("h2")) {
                            return;
                        } else {
                            getSQLiteManager().deleteFromNullBannedPlayers("-n", player);
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, time);
                        }
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.last-ban-removed").replace("%1$f", player)));
                        if(announceBan) {
                            Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.ban-ip.broadcast-message").replace("%2$f", player).replace("%3$f", reason).replace("%1$f", initiatorName)));
                        }
                        return;
                    }
                } else {
                    initiator.sendMessage(setColors(this.accessor.getLang().getString("other.already-banned") ));
                    return;
                }
            } else {
                if(getConfigSettings().isAllowedUseRamAsContainer()) {
                    if (getConfigSettings().getStorageType().equalsIgnoreCase("sqlite")) {
                        getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, time);
                    } else if (getConfigSettings().getStorageType().equalsIgnoreCase("mysql")) {
                        return;
                    } else if (getConfigSettings().getStorageType().equalsIgnoreCase("h2")) {
                        return;
                    } else {
                        getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, banPlayerEvent.getBanTime());
                    }
                    getBanContainerManager().addToBanContainer(id, "NULL_PLAYER", player, initiator.getName(), reason, type, realDate, realTime, "NULL_PLAYER", banPlayerEvent.getBanTime());
                    initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                    if(announceBan) {
                        Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.ban-ip.broadcast-message").replace("%2$f", player).replace("%3$f", reason).replace("%1$f", initiatorName)));
                    }
                    return;
                } else {
                    if (getConfigSettings().getStorageType().equalsIgnoreCase("sqlite")) {
                        getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, time);
                    } else if (getConfigSettings().getStorageType().equalsIgnoreCase("mysql")) {
                        return;
                    } else if (getConfigSettings().getStorageType().equalsIgnoreCase("h2")) {
                        return;
                    } else {
                        getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, banPlayerEvent.getBanTime());
                    }
                    initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                    if(announceBan) {
                        Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.ban-ip.broadcast-message").replace("%2$f", player).replace("%3$f", reason).replace("%1$f", initiatorName)));
                    }
                    return;
                }
            }
        }

        if(type == BanType.TIMED_IP) {
            if(isPlayerBanned(player)) {
                if(initiator.hasPermission("functionalbans.use.re-ban")) {
                    if (getConfigSettings().isAllowedUseRamAsContainer()) {
                        if(getConfigSettings().getStorageType().equalsIgnoreCase("sqlite")) {
                            getSQLiteManager().deleteFromBannedPlayers("-n", player);
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, time);
                        } else if(getConfigSettings().getStorageType().equalsIgnoreCase("h2")) {
                            return;
                        } else if(getConfigSettings().getStorageType().equalsIgnoreCase("mysql")) {
                            return;
                        } else {
                            getSQLiteManager().deleteFromBannedPlayers("-n", player);
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, time);
                        }
                        getBanContainerManager().removeFromBanContainer("-n", player);
                        getBanContainerManager().addToBanContainer(id, "NULL_PLAYER", player, initiator.getName(), reason, type, realDate, realTime, "NULL_PLAYER", time);
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.last-ban-removed").replace("%1$f", player)));
                        if(announceBan) {
                            Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban-ip.broadcast-message").replace("%1$f", initiatorName).replace("%2$f", player).replace("%3$f", this.timeManager.convertFromMillis(banPlayerEvent.getBanTime() - System.currentTimeMillis())).replace("%4$f", reason)));
                        }
                    } else {
                        if(getConfigSettings().getStorageType().equalsIgnoreCase("sqlite")) {
                            getSQLiteManager().deleteFromBannedPlayers("-n", player);
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, time);
                        } else if(getConfigSettings().getStorageType().equalsIgnoreCase("h2")) {
                            return;
                        } else if(getConfigSettings().getStorageType().equalsIgnoreCase("mysql")) {
                            return;
                        } else {
                            getSQLiteManager().deleteFromBannedPlayers("-n", player);
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, time);
                        }
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.last-ban-removed").replace("%1$f", player)));
                        if(announceBan) {
                            Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban-ip.broadcast-message").replace("%1$f", initiatorName).replace("%2$f", player).replace("%3$f", this.timeManager.convertFromMillis(banPlayerEvent.getBanTime() - System.currentTimeMillis())).replace("%4$f", reason)));
                        }
                    }
                } else {
                    initiator.sendMessage(setColors(this.accessor.getLang().getString("other.already-banned")));
                    return;
                }
            } else {
                if (getConfigSettings().isAllowedUseRamAsContainer()) {
                    if(getConfigSettings().getStorageType().equalsIgnoreCase("sqlite")) {
                        getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, time);
                    } else if(getConfigSettings().getStorageType().equalsIgnoreCase("h2")) {
                        return;
                    } else if(getConfigSettings().getStorageType().equalsIgnoreCase("mysql")) {
                        return;
                    } else {
                        getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, time);
                    }
                    getBanContainerManager().addToBanContainer(id, "NULL_PLAYER", player, initiator.getName(), reason, type, realDate, realTime, "NULL_PLAYER", time);
                    initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                    if(announceBan) {
                        Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban-ip.broadcast-message").replace("%1$f", initiatorName).replace("%2$f", player).replace("%3$f", this.timeManager.convertFromMillis(banPlayerEvent.getBanTime() - System.currentTimeMillis())).replace("%4$f", reason)));
                    }
                } else {
                    if(getConfigSettings().getStorageType().equalsIgnoreCase("sqlite")) {
                        getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, time);
                    } else if(getConfigSettings().getStorageType().equalsIgnoreCase("h2")) {
                        return;
                    } else if(getConfigSettings().getStorageType().equalsIgnoreCase("mysql")) {
                        return;
                    } else {
                        getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, time);
                    }
                    initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                    if(announceBan) {
                        Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban-ip.broadcast-message").replace("%1$f", initiatorName).replace("%2$f", player).replace("%3$f", this.timeManager.convertFromMillis(banPlayerEvent.getBanTime() - System.currentTimeMillis())).replace("%4$f", reason)));
                    }
                }
            }
        }

        if(type == BanType.TIMED_NOT_IP) {
            if(isPlayerBanned(player)) {
                if(initiator.hasPermission("functionalbans.use.re-ban")) {
                    if (getConfigSettings().isAllowedUseRamAsContainer()) {
                        if(getConfigSettings().getStorageType().equalsIgnoreCase("sqlite")) {
                            getSQLiteManager().deleteFromBannedPlayers("-n", player);
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, time);
                        } else if(getConfigSettings().getStorageType().equalsIgnoreCase("h2")) {
                            return;
                        } else if(getConfigSettings().getStorageType().equalsIgnoreCase("mysql")) {
                            return;
                        } else {
                            getSQLiteManager().deleteFromBannedPlayers("-n", player);
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, time);
                        }
                        getBanContainerManager().removeFromBanContainer("-n", player);
                        getBanContainerManager().addToBanContainer(id, "NULL_PLAYER", player, initiator.getName(), reason, type, realDate, realTime, "NULL_PLAYER", time);
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.last-ban-removed").replace("%1$f", player)));
                        if(announceBan) {
                            Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban.broadcast-message").replace("%1$f", initiatorName).replace("%2$f", player).replace("%3$f", this.timeManager.convertFromMillis(banPlayerEvent.getBanTime() - System.currentTimeMillis())).replace("%4$f", reason)));
                        }
                    } else {
                        if(getConfigSettings().getStorageType().equalsIgnoreCase("sqlite")) {
                            getSQLiteManager().deleteFromBannedPlayers("-n", player);
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, time);
                        } else if(getConfigSettings().getStorageType().equalsIgnoreCase("h2")) {
                            return;
                        } else if(getConfigSettings().getStorageType().equalsIgnoreCase("mysql")) {
                            return;
                        } else {
                            getSQLiteManager().deleteFromBannedPlayers("-n", player);
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, time);
                        }
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.last-ban-removed").replace("%1$f", player)));
                        if(announceBan) {
                            Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban.broadcast-message").replace("%1$f", initiatorName).replace("%2$f", player).replace("%3$f", this.timeManager.convertFromMillis(banPlayerEvent.getBanTime() - System.currentTimeMillis())).replace("%4$f", reason)));
                        }
                    }
                } else {
                    initiator.sendMessage(setColors(this.accessor.getLang().getString("other.already-banned")));
                    return;
                }
            } else {
                if (getConfigSettings().isAllowedUseRamAsContainer()) {
                    if(getConfigSettings().getStorageType().equalsIgnoreCase("sqlite")) {
                        getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, time);
                    } else if(getConfigSettings().getStorageType().equalsIgnoreCase("h2")) {
                        return;
                    } else if(getConfigSettings().getStorageType().equalsIgnoreCase("mysql")) {
                        return;
                    } else {
                        getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, time);
                    }
                    getBanContainerManager().addToBanContainer(id, "NULL_PLAYER", player, initiator.getName(), reason, type, realDate, realTime, "NULL_PLAYER", time);
                    initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                    if(announceBan) {
                        Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban.broadcast-message").replace("%1$f", initiatorName).replace("%2$f", player).replace("%3$f", this.timeManager.convertFromMillis(banPlayerEvent.getBanTime() - System.currentTimeMillis())).replace("%4$f", reason)));
                    }
                } else {
                    if(getConfigSettings().getStorageType().equalsIgnoreCase("sqlite")) {
                        getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, time);
                    } else if(getConfigSettings().getStorageType().equalsIgnoreCase("h2")) {
                        return;
                    } else if(getConfigSettings().getStorageType().equalsIgnoreCase("mysql")) {
                        return;
                    } else {
                        getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiator.getName(), reason, type, realDate, realTime, time);
                    }
                    initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                    if(announceBan) {
                        Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban.broadcast-message").replace("%1$f", initiatorName).replace("%2$f", player).replace("%3$f", this.timeManager.convertFromMillis(banPlayerEvent.getBanTime() - System.currentTimeMillis())).replace("%4$f", reason)));
                    }
                }
            }
        }

    }

    public BanContainerManager getBanContainerManager() {
        return banContainerManager;
    }
}
