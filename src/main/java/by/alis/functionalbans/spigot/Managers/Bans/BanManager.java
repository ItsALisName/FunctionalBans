package by.alis.functionalbans.spigot.Managers.Bans;

import by.alis.functionalbans.API.Spigot.Events.AsyncBanPreprocessEvent;
import by.alis.functionalbans.API.Enums.BanType;
import by.alis.functionalbans.spigot.Additional.WorldDate.WorldTimeAndDateClass;
import by.alis.functionalbans.spigot.FunctionalBansSpigot;
import by.alis.functionalbans.spigot.Managers.CooldownsManager;
import by.alis.functionalbans.spigot.Managers.IdsManager;
import by.alis.functionalbans.spigot.Managers.Files.FileAccessor;
import by.alis.functionalbans.spigot.Managers.TimeManagers.TimeManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static by.alis.functionalbans.spigot.Additional.GlobalSettings.StaticSettingsAccessor.*;
import static by.alis.functionalbans.spigot.Additional.Other.TextUtils.setColors;
import static by.alis.functionalbans.databases.StaticBases.getSQLiteManager;
import static by.alis.functionalbans.spigot.Managers.Bans.BanChecker.isIpBanned;
import static by.alis.functionalbans.spigot.Managers.Bans.BanChecker.isPlayerBanned;

/**
 * The class responsible for the new account ban method
 */
public class BanManager {

    FileAccessor accessor = new FileAccessor();
    IdsManager idsManager = new IdsManager();
    TimeManager timeManager = new TimeManager();

    BanContainerManager banContainerManager = new BanContainerManager();

    /**
     * Blocks a player who has ever played on the server (i.e. not a null player)
     * @param player player
     * @param type Ban type
     * @param reason Ban reason
     * @param initiator Who blocked
     * @param time Ban time
     * @param announceBan If true, then the blocking will be notified in the chat
     */
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
            convertedTime = this.timeManager.convertFromMillis(this.timeManager.getBanTime(time));
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

        if(!announceBan) {
            if(!initiator.hasPermission("functionalbans.use.silently")) {
                initiator.sendMessage(setColors(this.accessor.getLang().getString("other.flag-no-perms").replace("%1$f", "-s")));
                banPlayerEvent.setCancelled(true);
                return;
            }
        }

        if(reason.equalsIgnoreCase(getGlobalVariables().getDefaultReason())) {
            if(!getConfigSettings().isBanAllowedWithoutReason() && !initiator.hasPermission("functionalbans.use.no-reason")) {
                initiator.sendMessage(setColors(this.accessor.getLang().getString("other.no-reason")));
                banPlayerEvent.setCancelled(true);
                return;
            }
        }

        if(initiator instanceof Player) {
            if (!initiator.hasPermission("functionalbans.time-bypass")) {
                if (time > this.timeManager.getMaxPlayerPunishTime((Player) initiator)) {
                    initiator.sendMessage(setColors(this.accessor.getLang().getString("other.ban-over-time").replace("%1$f", this.timeManager.convertFromMillis(this.timeManager.getMaxPlayerPunishTime((Player) initiator)))));
                    banPlayerEvent.setCancelled(true);
                    return;
                }
            }
        }

        if(initiator instanceof Player) {
            if(CooldownsManager.playerHasCooldown(((Player) initiator).getPlayer(), "ban")) {
                CooldownsManager.notifyAboutCooldown(((Player) initiator).getPlayer(), "ban");
                banPlayerEvent.setCancelled(true);
                return;
            } else {
                CooldownsManager.setCooldown(((Player) initiator).getPlayer(), "ban");
            }
        }

        if(getConfigSettings().isProhibitYourselfInteraction()) {
            if(initiator.getName().equalsIgnoreCase(player.getName())) {
                initiator.sendMessage(setColors(this.accessor.getLang().getString("other.no-yourself-actions")));
                banPlayerEvent.setCancelled(true);
                return;
            }
        }

        reason = banPlayerEvent.getReason().replace("'", "\"");
        String initiatorName = null;
        if(initiator instanceof Player) {
            initiatorName = ((Player)initiator).getPlayerListName();
        } else {
            initiatorName = getGlobalVariables().getConsoleVariableName();
        }


        if(type == BanType.PERMANENT_NOT_IP) {
            if (isPlayerBanned(player)) {
                if (initiator.hasPermission("functionalbans.use.re-ban")) {
                    if (getConfigSettings().isAllowedUseRamAsContainer()) {
                        switch (getConfigSettings().getStorageType()) {
                            case SQLITE: {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ingored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ingored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), -1);
                                break;
                            }
                            case MYSQL: {
                                break;
                            }
                            case H2: {
                                break;
                            }
                            default: {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ingored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ingored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), -1);
                                break;
                            }
                        }
                        try { getBanContainerManager().removeFromBanContainer("-u", String.valueOf(player.getUniqueId())); } catch (NullPointerException ingored) {}
                        try { getBanContainerManager().removeFromBanContainer("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId())); } catch (NullPointerException ignored) {}
                        getBanContainerManager().addToBanContainer(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, String.valueOf(player.getUniqueId()), -1L);
                        if(player.isOnline()) {
                            player.getPlayer().kickPlayer(setColors(String.join("\n", this.accessor.getLang().getStringList("ban-message-format")).replace("%1$f", String.valueOf(id)).replace("%2$f", reason).replace("%3$f", initiatorName).replace("%4$f", realDate + ", " + realTime).replace("%5$f", getGlobalVariables().getVariableNever())));                            }
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.last-ban-removed").replace("%1$f", player.getName())));
                        if(announceBan) {
                            Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.ban.broadcast-message").replace("%2$f", player.getName()).replace("%3$f", reason).replace("%1$f", initiatorName)));
                        }
                        return;
                    } else {
                        switch (getConfigSettings().getStorageType()) {
                            case SQLITE: {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ingored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ingored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), -1);
                                break;
                            }
                            case MYSQL: {
                                break;
                            }
                            case H2: {
                                break;
                            }
                            default: {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ingored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ingored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), -1);
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
                        case SQLITE: {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), -1);
                            break;
                        }
                        case MYSQL: {
                            break;
                        }
                        case H2: {
                            break;
                        }
                        default: {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), -1);
                            break;
                        }
                    }
                    getBanContainerManager().addToBanContainer(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, String.valueOf(player.getUniqueId()), -1L);
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
                        case SQLITE: {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), -1);
                            break;
                        }
                        case MYSQL: {
                            break;
                        }
                        case H2: {
                            break;
                        }
                        default: {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), -1);
                            break;
                        }
                    }
                    getBanContainerManager().addToBanContainer(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, String.valueOf(player.getUniqueId()), -1L);
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
                            case SQLITE: {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), -1);
                                break;
                            }
                            case MYSQL: {
                                break;
                            }
                            case H2: {
                                break;
                            }
                            default: {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), -1);
                                break;
                            }
                        }
                        try { getBanContainerManager().removeFromBanContainer("-u", String.valueOf(player.getUniqueId())); } catch (NullPointerException ingored) {}
                        try { getBanContainerManager().removeFromBanContainer("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId())); } catch (NullPointerException ignored) {}
                        getBanContainerManager().addToBanContainer(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, String.valueOf(player.getUniqueId()), -1L);
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
                            case SQLITE: {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), -1);
                                break;
                            }
                            case MYSQL: {
                                break;
                            }
                            case H2: {
                                break;
                            }
                            default: {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), -1);
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
                        case SQLITE: {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), -1);
                            break;
                        }
                        case MYSQL: {
                            break;
                        }
                        case H2: {
                            break;
                        }
                        default: {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), -1);
                            break;
                        }
                    }
                    getBanContainerManager().addToBanContainer(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, String.valueOf(player.getUniqueId()), -1L);
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
                        case SQLITE: {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), -1);
                            break;
                        }
                        case MYSQL: {
                            break;
                        }
                        case H2: {
                            break;
                        }
                        default: {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), -1);
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
                            case SQLITE: {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                                break;
                            }
                            case MYSQL: {
                                break;
                            }
                            case H2: {
                                break;
                            }
                            default: {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                                break;
                            }
                        }
                        try { getBanContainerManager().removeFromBanContainer("-u", String.valueOf(player.getUniqueId())); } catch (NullPointerException ignored) {}
                        try { getBanContainerManager().removeFromBanContainer("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId())); } catch (NullPointerException ignored) {}
                        getBanContainerManager().addToBanContainer(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, String.valueOf(player.getUniqueId()), banPlayerEvent.getBanTime());
                        if(player.isOnline()) {
                            String finalInitiatorName = initiatorName;
                            String finalReason = reason;
                            Bukkit.getScheduler().runTask(FunctionalBansSpigot.getProvidingPlugin(FunctionalBansSpigot.class), () -> {
                                player.getPlayer().kickPlayer(setColors(String.join("\n", this.accessor.getLang().getStringList("temporary-ban-message-format")).replace("%1$f", String.valueOf(id)).replace("%2$f", finalReason).replace("%3$f", finalInitiatorName).replace("%4$f", realDate + ", " + realTime).replace("%5$f", this.timeManager.convertFromMillis(this.timeManager.getBanTime(banPlayerEvent.getBanTime())))));
                            });
                        }
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.last-ban-removed").replace("%1$f", player.getName())));
                        if(announceBan) {
                            Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban.broadcast-message").replace("%2$f", player.getName()).replace("%3$f", this.timeManager.convertFromMillis(this.timeManager.getBanTime(banPlayerEvent.getBanTime()))).replace("%1$f", initiatorName).replace("%4$f", reason)));
                        }
                        return;
                    } else {
                        switch (getConfigSettings().getStorageType()) {
                            case SQLITE: {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                                break;
                            }
                            case MYSQL: {
                                break;
                            }
                            case H2: {
                                break;
                            }
                            default: {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                                break;
                            }
                        }
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.last-ban-removed").replace("%1$f", player.getName())));
                        if(player.isOnline()) {
                            String finalReason = reason;
                            String finalInitiatorName = initiatorName;
                            Bukkit.getScheduler().runTask(FunctionalBansSpigot.getProvidingPlugin(FunctionalBansSpigot.class), () -> {
                                player.getPlayer().kickPlayer(setColors(String.join("\n", this.accessor.getLang().getStringList("temporary-ban-message-format")).replace("%1$f", String.valueOf(id)).replace("%2$f", finalReason).replace("%3$f", finalInitiatorName).replace("%4$f", realDate + ", " + realTime).replace("%5$f", this.timeManager.convertFromMillis(this.timeManager.getBanTime(banPlayerEvent.getBanTime())))));
                            });
                        }
                        if(announceBan) {
                            Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban.broadcast-message").replace("%2$f", player.getName()).replace("%3$f", this.timeManager.convertFromMillis(this.timeManager.getBanTime(banPlayerEvent.getBanTime()))).replace("%1$f", initiatorName).replace("%4$f", reason)));
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
                        case SQLITE: {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                            break;
                        }
                        case MYSQL: {
                            break;
                        }
                        case H2: {
                            break;
                        }
                        default: {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                            break;
                        }
                    }
                    getBanContainerManager().addToBanContainer(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, String.valueOf(player.getUniqueId()), banPlayerEvent.getBanTime());
                    if(announceBan) {
                        Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban.broadcast-message").replace("%2$f", player.getName()).replace("%3$f", this.timeManager.convertFromMillis(this.timeManager.getBanTime(banPlayerEvent.getBanTime()))).replace("%1$f", initiatorName).replace("%4$f", reason)));
                    }
                    if(player.isOnline()) {
                        String finalReason = reason;
                        String finalInitiatorName = initiatorName;
                        Bukkit.getScheduler().runTask(FunctionalBansSpigot.getProvidingPlugin(FunctionalBansSpigot.class), () -> {
                            player.getPlayer().kickPlayer(setColors(String.join("\n", this.accessor.getLang().getStringList("temporary-ban-message-format")).replace("%1$f", String.valueOf(id)).replace("%2$f", finalReason).replace("%3$f", finalInitiatorName).replace("%4$f", realDate + ", " + realTime).replace("%5$f", this.timeManager.convertFromMillis(this.timeManager.getBanTime(banPlayerEvent.getBanTime())))));
                        });
                    }
                } else {
                    switch (getConfigSettings().getStorageType()) {
                        case SQLITE: {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                            break;
                        }
                        case MYSQL: {
                            break;
                        }
                        case H2: {
                            break;
                        }
                        default: {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                            break;
                        }
                    }
                    if(player.isOnline()) {
                        String finalReason = reason;
                        String finalInitiatorName = initiatorName;
                        Bukkit.getScheduler().runTask(FunctionalBansSpigot.getProvidingPlugin(FunctionalBansSpigot.class), () -> {
                            player.getPlayer().kickPlayer(setColors(String.join("\n", this.accessor.getLang().getStringList("temporary-ban-message-format")).replace("%1$f", String.valueOf(id)).replace("%2$f", finalReason).replace("%3$f", finalInitiatorName).replace("%4$f", realDate + ", " + realTime).replace("%5$f", this.timeManager.convertFromMillis(this.timeManager.getBanTime(banPlayerEvent.getBanTime())))));
                        });
                    }
                    if(announceBan) {
                        Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban.broadcast-message").replace("%2$f", player.getName()).replace("%3$f", this.timeManager.convertFromMillis(this.timeManager.getBanTime(banPlayerEvent.getBanTime()))).replace("%1$f", initiatorName).replace("%4$f", reason)));
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
                            case SQLITE: {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                                break;
                            }
                            case MYSQL: {
                                break;
                            }
                            case H2: {
                                break;
                            }
                            default: {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                                break;
                            }
                        }
                        try { getBanContainerManager().removeFromBanContainer("-u", String.valueOf(player.getUniqueId())); } catch (NullPointerException ignored) {}
                        try { getBanContainerManager().removeFromBanContainer("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId())); } catch (NullPointerException ignored) {}
                        getBanContainerManager().addToBanContainer(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, String.valueOf(player.getUniqueId()), banPlayerEvent.getBanTime());
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.last-ban-removed").replace("%1$f", player.getName())));
                        if(announceBan) {
                            Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban-ip.broadcast-message").replace("%1$f", initiatorName).replace("%2$f", player.getName()).replace("%3$f", this.timeManager.convertFromMillis(this.timeManager.getBanTime(banPlayerEvent.getBanTime()))).replace("%4$f", reason)));
                        }
                        if(player.isOnline()) {
                            String finalReason = reason;
                            String finalInitiatorName = initiatorName;
                            Bukkit.getScheduler().runTask(FunctionalBansSpigot.getProvidingPlugin(FunctionalBansSpigot.class), () -> {
                                player.getPlayer().kickPlayer(setColors(String.join("\n", this.accessor.getLang().getStringList("temporary-ban-ip-message-format")).replace("%1$f", id).replace("%2$f", finalReason).replace("%3$f", finalInitiatorName)).replace("%4$f", realDate + ", " + realTime).replace("%5$f", this.timeManager.convertFromMillis(this.timeManager.getBanTime(banPlayerEvent.getBanTime()))));
                            });
                        }
                    } else {
                        switch (getConfigSettings().getStorageType()) {
                            case SQLITE: {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ingored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                                break;
                            }
                            case MYSQL: {
                                break;
                            }
                            case H2: {
                                break;
                            }
                            default: {
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-u", String.valueOf(player.getUniqueId()));} catch (NullPointerException ignored) {}
                                try {
                                    getSQLiteManager().deleteFromBannedPlayers("-ip", getSQLiteManager().selectIpByUUID(player.getUniqueId()));} catch (NullPointerException ingored) {}
                                getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                                break;
                            }
                        }
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.last-ban-removed").replace("%1$f", player.getName())));
                        if(announceBan) {
                            Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban-ip.broadcast-message").replace("%1$f", initiatorName).replace("%2$f", player.getName()).replace("%3$f", this.timeManager.convertFromMillis(this.timeManager.getBanTime(banPlayerEvent.getBanTime()))).replace("%4$f", reason)));
                        }
                        if(player.isOnline()) {
                            String finalReason = reason;
                            String finalInitiatorName = initiatorName;
                            Bukkit.getScheduler().runTask(FunctionalBansSpigot.getProvidingPlugin(FunctionalBansSpigot.class), () -> {
                                player.getPlayer().kickPlayer(setColors(String.join("\n", this.accessor.getLang().getStringList("ban-ip-message-format")).replace("%1$f", id).replace("%2$f", finalReason).replace("%3$f", finalInitiatorName)).replace("%4$f", realDate + ", " + realTime).replace("%5$f", this.timeManager.convertFromMillis(this.timeManager.getBanTime(banPlayerEvent.getBanTime()))));
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
                        case SQLITE: {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                            break;
                        }
                        case MYSQL: {
                            break;
                        }
                        case H2: {
                            break;
                        }
                        default: {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                            break;
                        }
                    }
                    getBanContainerManager().addToBanContainer(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, String.valueOf(player.getUniqueId()), banPlayerEvent.getBanTime());
                    if(announceBan) {
                        Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban-ip.broadcast-message").replace("%1$f", initiatorName).replace("%2$f", player.getName()).replace("%3$f", this.timeManager.convertFromMillis(this.timeManager.getBanTime(banPlayerEvent.getBanTime()))).replace("%4$f", reason)));
                    }
                    if(player.isOnline()) {
                        String finalInitiatorName = initiatorName;
                        String finalReason = reason;
                        Bukkit.getScheduler().runTask(FunctionalBansSpigot.getProvidingPlugin(FunctionalBansSpigot.class), () -> {
                            player.getPlayer().kickPlayer(setColors(String.join("\n", this.accessor.getLang().getStringList("temporary-ban-ip-message-format")).replace("%1$f", id).replace("%2$f", finalReason).replace("%3$f", finalInitiatorName)).replace("%4$f", realDate + ", " + realTime).replace("%5$f", this.timeManager.convertFromMillis(this.timeManager.getBanTime(banPlayerEvent.getBanTime()))));
                        });
                    }
                } else {
                    switch (getConfigSettings().getStorageType()) {
                        case SQLITE: {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                            break;
                        }
                        case MYSQL: {
                            break;
                        }
                        case H2: {
                            break;
                        }
                        default: {
                            getSQLiteManager().insertIntoBannedPlayers(id, getSQLiteManager().selectIpByUUID(player.getUniqueId()), player.getName(), initiatorName, reason, type, realDate, realTime, player.getUniqueId(), banPlayerEvent.getBanTime());
                            break;
                        }
                    }
                    if(announceBan) {
                        Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban-ip.broadcast-message").replace("%1$f", initiatorName).replace("%2$f", player.getName()).replace("%3$f", this.timeManager.convertFromMillis(this.timeManager.getBanTime(banPlayerEvent.getBanTime()))).replace("%4$f", reason)));
                    }
                    if(player.isOnline()) {
                        String finalInitiatorName = initiatorName;
                        String finalReason = reason;
                        Bukkit.getScheduler().runTask(FunctionalBansSpigot.getProvidingPlugin(FunctionalBansSpigot.class), () -> {
                            player.getPlayer().kickPlayer(setColors(String.join("\n", this.accessor.getLang().getStringList("temporary-ban-ip-message-format")).replace("%1$f", id).replace("%2$f", finalReason).replace("%3$f", finalInitiatorName)).replace("%4$f", realDate + ", " + realTime).replace("%5$f", this.timeManager.convertFromMillis(this.timeManager.getBanTime(banPlayerEvent.getBanTime()))));
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
        if (time < 0) {
            convertedTime = getGlobalVariables().getVariableNever();
        } else {
            convertedTime = this.timeManager.convertFromMillis(time - System.currentTimeMillis());
        }
        String id = idsManager.getId();
        if (reason == null || reason.equalsIgnoreCase("")) {
            reason = getGlobalVariables().getDefaultReason();
        }
        AsyncBanPreprocessEvent banPlayerEvent = new AsyncBanPreprocessEvent(id, player, initiator, type, time, reason, realTime, realDate, getConfigSettings().isApiEnabled(), convertedTime);
        reason = banPlayerEvent.getReason().replace("'", "\"");
        time = banPlayerEvent.getBanTime();
        if (type == BanType.PERMANENT_IP || type == BanType.PERMANENT_NOT_IP) time = -1;
        if (getConfigSettings().isApiEnabled()) {
            if (!getConfigSettings().isApiProtectedByPassword()) {
                Bukkit.getPluginManager().callEvent(banPlayerEvent);
            } else {
                if (banPlayerEvent.getApiPassword() != null && banPlayerEvent.getApiPassword().equalsIgnoreCase(this.accessor.getGeneralConfig().getString("plugin-settings.api.spigot.password.password"))) {
                    Bukkit.getPluginManager().callEvent(banPlayerEvent);
                }
            }
        }
        if (banPlayerEvent.isCancelled()) return;

        if(!announceBan) {
            if(!initiator.hasPermission("functionalbans.use.silently")) {
                initiator.sendMessage(setColors(this.accessor.getLang().getString("other.flag-no-perms").replace("%1$f", "-s")));
                banPlayerEvent.setCancelled(true);
                return;
            }
        }

        if(reason.equalsIgnoreCase(getGlobalVariables().getDefaultReason())) {
            if(!getConfigSettings().isBanAllowedWithoutReason() && !initiator.hasPermission("functionalbans.use.no-reason")) {
                initiator.sendMessage(setColors(this.accessor.getLang().getString("other.no-reason")));
                banPlayerEvent.setCancelled(true);
                return;
            }
        }

        if(initiator instanceof Player) {
            if (!initiator.hasPermission("functionalbans.time-bypass")) {
                if (time > this.timeManager.getMaxPlayerPunishTime((Player) initiator)) {
                    initiator.sendMessage(setColors(this.accessor.getLang().getString("other.ban-over-time").replace("%1$f", this.timeManager.convertFromMillis(this.timeManager.getMaxPlayerPunishTime((Player) initiator)))));
                    banPlayerEvent.setCancelled(true);
                    return;
                }
            }
        }

        if(initiator instanceof Player) {
            if(CooldownsManager.playerHasCooldown(((Player) initiator).getPlayer(), "ban")) {
                CooldownsManager.notifyAboutCooldown(((Player) initiator).getPlayer(), "ban");
                banPlayerEvent.setCancelled(true);
                return;
            } else {
                CooldownsManager.setCooldown(((Player) initiator).getPlayer(), "ban");
            }
        }

        if(getConfigSettings().isProhibitYourselfInteraction()) {
            if(initiator.getName().equalsIgnoreCase(player)) {
                initiator.sendMessage(setColors(this.accessor.getLang().getString("other.no-yourself-actions")));
                banPlayerEvent.setCancelled(true);
                return;
            }
        }

        String initiatorName = "ERROR";
        if (initiator instanceof Player) {
            initiatorName = ((Player) initiator).getPlayerListName();
        } else {
            initiatorName = getGlobalVariables().getConsoleVariableName();
        }

        if (type == BanType.PERMANENT_NOT_IP) {
            if (isPlayerBanned(player)) {
                if (initiator.hasPermission("functionalbans.use.re-ban")) {
                    if (getConfigSettings().isAllowedUseRamAsContainer()) {
                        switch (getConfigSettings().getStorageType()) {
                            case SQLITE: {
                                getSQLiteManager().deleteFromNullBannedPlayers("-n", player);
                                getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                                break;
                            }
                            case MYSQL: {
                                break;
                            }
                            case H2: {
                                break;
                            }
                            default: {
                                getSQLiteManager().deleteFromNullBannedPlayers("-n", player);
                                getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                                break;
                            }
                        }
                        getBanContainerManager().removeFromBanContainer("-n", player);
                        getBanContainerManager().addToBanContainer(id, "NULL_PLAYER", player, initiatorName, reason, type, realDate, realTime, "NULL_PLAYER", time);
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.last-ban-removed").replace("%1$f", player)));
                        if (announceBan) {
                            Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.ban.broadcast-message").replace("%2$f", player).replace("%3$f", reason).replace("%1$f", initiatorName)));
                        }
                        return;
                    } else {
                        switch (getConfigSettings().getStorageType()) {
                            case SQLITE: {
                                getSQLiteManager().deleteFromNullBannedPlayers("-n", player);
                                getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                                break;
                            }
                            case MYSQL: {
                                break;
                            }
                            case H2: {
                                break;
                            }
                            default: {
                                getSQLiteManager().deleteFromNullBannedPlayers("-n", player);
                                getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                                break;
                            }
                        }
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.last-ban-removed").replace("%1$f", player)));
                        if (announceBan) {
                            Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.ban.broadcast-message").replace("%2$f", player).replace("%3$f", reason).replace("%1$f", initiatorName)));
                        }
                        return;
                    }
                } else {
                    initiator.sendMessage(setColors(this.accessor.getLang().getString("other.already-banned")));
                    return;
                }
            } else {
                if (getConfigSettings().isAllowedUseRamAsContainer()) {
                    switch (getConfigSettings().getStorageType()) {
                        case SQLITE: {
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                            break;
                        }
                        case MYSQL: {
                            break;
                        }
                        case H2: {
                            break;
                        }
                        default: {
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                            break;
                        }
                    }
                    getBanContainerManager().addToBanContainer(id, "NULL_PLAYER", player, initiatorName, reason, type, realDate, realTime, "NULL_PLAYER", time);
                    initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                    if (announceBan) {
                        Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.ban.broadcast-message").replace("%2$f", player).replace("%3$f", reason).replace("%1$f", initiatorName)));
                    }
                    return;
                } else {
                    switch (getConfigSettings().getStorageType()) {
                        case SQLITE: {
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                            break;
                        }
                        case MYSQL: {
                            break;
                        }
                        case H2: {
                            break;
                        }
                        default: {
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                            break;
                        }
                    }
                    initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                    if (announceBan) {
                        Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.ban.broadcast-message").replace("%2$f", player).replace("%3$f", reason).replace("%1$f", initiatorName)));
                    }
                    return;
                }
            }
        }

        if (type == BanType.PERMANENT_IP) {
            if (isPlayerBanned(player)) {
                if (initiator.hasPermission("functionalbans.use.re-ban")) {
                    if (getConfigSettings().isAllowedUseRamAsContainer()) {
                        switch (getConfigSettings().getStorageType()) {
                            case SQLITE: {
                                getSQLiteManager().deleteFromNullBannedPlayers("-n", player);
                                getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                                break;
                            }
                            case MYSQL: {
                                break;
                            }
                            case H2: {
                                break;
                            }
                            default: {
                                getSQLiteManager().deleteFromNullBannedPlayers("-n", player);
                                getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                                break;
                            }
                        }
                        getBanContainerManager().removeFromBanContainer("-n", player);
                        getBanContainerManager().addToBanContainer(id, "NULL_PLAYER", player, initiatorName, reason, type, realDate, realTime, "NULL_PLAYER", banPlayerEvent.getBanTime());
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.last-ban-removed").replace("%1$f", player)));
                        if (announceBan) {
                            Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.ban-ip.broadcast-message").replace("%2$f", player).replace("%3$f", reason).replace("%1$f", initiatorName)));
                        }
                        return;
                    } else {
                        switch (getConfigSettings().getStorageType()) {
                            case SQLITE: {
                                getSQLiteManager().deleteFromNullBannedPlayers("-n", player);
                                getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                                break;
                            }
                            case MYSQL: {
                                break;
                            }
                            case H2: {
                                break;
                            }
                            default: {
                                getSQLiteManager().deleteFromNullBannedPlayers("-n", player);
                                getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                                break;
                            }
                        }
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.last-ban-removed").replace("%1$f", player)));
                        if (announceBan) {
                            Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.ban-ip.broadcast-message").replace("%2$f", player).replace("%3$f", reason).replace("%1$f", initiatorName)));
                        }
                        return;
                    }
                } else {
                    initiator.sendMessage(setColors(this.accessor.getLang().getString("other.already-banned")));
                    return;
                }
            } else {
                if (getConfigSettings().isAllowedUseRamAsContainer()) {
                    switch (getConfigSettings().getStorageType()) {
                        case SQLITE: {
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                            break;
                        }
                        case MYSQL: {
                            break;
                        }
                        case H2: {
                            break;
                        }
                        default: {
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                            break;
                        }
                    }
                    getBanContainerManager().addToBanContainer(id, "NULL_PLAYER", player, initiatorName, reason, type, realDate, realTime, "NULL_PLAYER", banPlayerEvent.getBanTime());
                    initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                    if (announceBan) {
                        Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.ban-ip.broadcast-message").replace("%2$f", player).replace("%3$f", reason).replace("%1$f", initiatorName)));
                    }
                    return;
                } else {
                    switch (getConfigSettings().getStorageType()) {
                        case SQLITE: {
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                            break;
                        }
                        case MYSQL: {
                            break;
                        }
                        case H2: {
                            break;
                        }
                        default: {
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                            break;
                        }
                    }
                    initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                    if (announceBan) {
                        Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.ban-ip.broadcast-message").replace("%2$f", player).replace("%3$f", reason).replace("%1$f", initiatorName)));
                    }
                    return;
                }
            }
        }

        if (type == BanType.TIMED_IP) {
            if (isPlayerBanned(player)) {
                if (initiator.hasPermission("functionalbans.use.re-ban")) {
                    if (getConfigSettings().isAllowedUseRamAsContainer()) {
                        switch (getConfigSettings().getStorageType()) {
                            case SQLITE: {
                                getSQLiteManager().deleteFromBannedPlayers("-n", player);
                                getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                                break;
                            }
                            case MYSQL: {
                                break;
                            }
                            case H2: {
                                break;
                            }
                            default: {
                                getSQLiteManager().deleteFromBannedPlayers("-n", player);
                                getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                                break;
                            }
                        }
                        getBanContainerManager().removeFromBanContainer("-n", player);
                        getBanContainerManager().addToBanContainer(id, "NULL_PLAYER", player, initiatorName, reason, type, realDate, realTime, "NULL_PLAYER", time);
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.last-ban-removed").replace("%1$f", player)));
                        if (announceBan) {
                            Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban-ip.broadcast-message").replace("%1$f", initiatorName).replace("%2$f", player).replace("%3$f", this.timeManager.convertFromMillis(this.timeManager.getBanTime(banPlayerEvent.getBanTime()))).replace("%4$f", reason)));
                        }
                    } else {
                        switch (getConfigSettings().getStorageType()) {
                            case SQLITE: {
                                getSQLiteManager().deleteFromBannedPlayers("-n", player);
                                getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                            }
                            case MYSQL: {
                                break;
                            }
                            case H2: {
                                break;
                            }
                            default: {
                                getSQLiteManager().deleteFromBannedPlayers("-n", player);
                                getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                            }
                        }
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.last-ban-removed").replace("%1$f", player)));
                        if (announceBan) {
                            Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban-ip.broadcast-message").replace("%1$f", initiatorName).replace("%2$f", player).replace("%3$f", this.timeManager.convertFromMillis(this.timeManager.getBanTime(banPlayerEvent.getBanTime()))).replace("%4$f", reason)));
                        }
                    }
                } else {
                    initiator.sendMessage(setColors(this.accessor.getLang().getString("other.already-banned")));
                    return;
                }
            } else {
                if (getConfigSettings().isAllowedUseRamAsContainer()) {
                    switch (getConfigSettings().getStorageType()) {
                        case SQLITE: {
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                            break;
                        }
                        case MYSQL: {
                            break;
                        }
                        case H2: {
                            break;
                        }
                        default: {
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                            break;
                        }
                    }
                    getBanContainerManager().addToBanContainer(id, "NULL_PLAYER", player, initiatorName, reason, type, realDate, realTime, "NULL_PLAYER", time);
                    initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                    if (announceBan) {
                        Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban-ip.broadcast-message").replace("%1$f", initiatorName).replace("%2$f", player).replace("%3$f", this.timeManager.convertFromMillis(this.timeManager.getBanTime(banPlayerEvent.getBanTime()))).replace("%4$f", reason)));
                    }
                } else {
                    switch (getConfigSettings().getStorageType()) {
                        case SQLITE: {
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                            break;
                        }
                        case MYSQL: {
                            break;
                        }
                        case H2: {
                            break;
                        }
                        default: {
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                            break;
                        }
                    }
                    initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                    if (announceBan) {
                        Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban-ip.broadcast-message").replace("%1$f", initiatorName).replace("%2$f", player).replace("%3$f", this.timeManager.convertFromMillis(this.timeManager.getBanTime(banPlayerEvent.getBanTime()))).replace("%4$f", reason)));
                    }
                }
            }
        }

        if (type == BanType.TIMED_NOT_IP) {
            if (isPlayerBanned(player)) {
                if (initiator.hasPermission("functionalbans.use.re-ban")) {
                    if (getConfigSettings().isAllowedUseRamAsContainer()) {
                        switch (getConfigSettings().getStorageType()) {
                            case SQLITE: {
                                getSQLiteManager().deleteFromBannedPlayers("-n", player);
                                getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                                break;
                            }
                            case MYSQL: {
                                break;
                            }
                            case H2: {
                                break;
                            }
                            default: {
                                getSQLiteManager().deleteFromBannedPlayers("-n", player);
                                getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                                break;
                            }
                        }
                        getBanContainerManager().removeFromBanContainer("-n", player);
                        getBanContainerManager().addToBanContainer(id, "NULL_PLAYER", player, initiatorName, reason, type, realDate, realTime, "NULL_PLAYER", time);
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.last-ban-removed").replace("%1$f", player)));
                        if (announceBan) {
                            Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban.broadcast-message").replace("%1$f", initiatorName).replace("%2$f", player).replace("%3$f", this.timeManager.convertFromMillis(this.timeManager.getBanTime(banPlayerEvent.getBanTime()))).replace("%4$f", reason)));
                        }
                    } else {
                        switch (getConfigSettings().getStorageType()) {
                            case SQLITE: {
                                getSQLiteManager().deleteFromBannedPlayers("-n", player);
                                getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                                break;
                            }
                            case MYSQL: {
                                break;
                            }
                            case H2: {
                                break;
                            }
                            default: {
                                getSQLiteManager().deleteFromBannedPlayers("-n", player);
                                getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                                break;
                            }
                        }
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                        initiator.sendMessage(setColors(this.accessor.getLang().getString("other.last-ban-removed").replace("%1$f", player)));
                        if (announceBan) {
                            Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban.broadcast-message").replace("%1$f", initiatorName).replace("%2$f", player).replace("%3$f", this.timeManager.convertFromMillis(this.timeManager.getBanTime(banPlayerEvent.getBanTime()))).replace("%4$f", reason)));
                        }
                    }
                } else {
                    initiator.sendMessage(setColors(this.accessor.getLang().getString("other.already-banned")));
                    return;
                }
            } else {
                if (getConfigSettings().isAllowedUseRamAsContainer()) {
                    switch (getConfigSettings().getStorageType()) {
                        case SQLITE: {
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                            break;
                        }
                        case MYSQL: {
                            break;
                        }
                        case H2: {
                            break;
                        }
                        default: {
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                            break;
                        }
                    }
                    getBanContainerManager().addToBanContainer(id, "NULL_PLAYER", player, initiatorName, reason, type, realDate, realTime, "NULL_PLAYER", time);
                    initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                    if (announceBan) {
                        Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban.broadcast-message").replace("%1$f", initiatorName).replace("%2$f", player).replace("%3$f", this.timeManager.convertFromMillis(this.timeManager.getBanTime(banPlayerEvent.getBanTime()))).replace("%4$f", reason)));
                    }
                } else {
                    switch (getConfigSettings().getStorageType()) {
                        case SQLITE: {
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                            break;
                        }
                        case MYSQL: {
                            break;
                        }
                        case H2: {
                            break;
                        }
                        default: {
                            getSQLiteManager().insertIntoNullBannedPlayers(id, player, initiatorName, reason, type, realDate, realTime, time);
                            break;
                        }
                    }
                    initiator.sendMessage(setColors(this.accessor.getLang().getString("other.unknown-player").replace("%1$f", player)));
                    if (announceBan) {
                        Bukkit.broadcastMessage(setColors(this.accessor.getLang().getString("commands.tempban.broadcast-message").replace("%1$f", initiatorName).replace("%2$f", player).replace("%3$f", this.timeManager.convertFromMillis(this.timeManager.getBanTime(banPlayerEvent.getBanTime()))).replace("%4$f", reason)));
                    }
                }
            }
        }

    }

    public BanContainerManager getBanContainerManager() {
        return banContainerManager;
    }
}