package by.alis.functionalbans.spigot.Additional.GlobalSettings;

import by.alis.functionalbans.spigot.Additional.Containers.StaticContainers;
import by.alis.functionalbans.spigot.Additional.GlobalSettings.Languages.LangEnglish;
import by.alis.functionalbans.spigot.Additional.GlobalSettings.Languages.LangRussian;
import by.alis.functionalbans.spigot.Managers.FilesManagers.FileAccessor;
import org.bukkit.Bukkit;

import java.util.HashSet;
import java.util.Set;

import static by.alis.functionalbans.spigot.Additional.Other.TextUtils.setColors;

public class GeneralConfigSettings {

    private final FileAccessor fileAccessor = new FileAccessor();
    private String consoleLanguageMode = "en_US";
    private boolean isAnnounceWhenLogHided = true;
    private boolean isAllowedUseRamAsContainer = false;
    private boolean unsafeActionsConfirmation = true;
    private boolean isBanAllowedWithoutReason = true;
    private boolean isKickAllowedWithoutReason = true;
    private boolean showDescription = true;
    private boolean purgeConfirmation = true;
    private boolean isMuteAllowedWithoutReason = true;
    private boolean isLessInformation = false;
    private boolean hideMainCommand = false;
    private boolean showExamples = true;
    private Set<String> timeRestrictionGroups = new HashSet<>();
    private boolean isApiEnabled = true;
    private String storageType = "sqlite";
    private boolean isApiProtectedByPassword = false;
    private boolean isProhibitYourselfInteraction = false;



    public boolean isUnsafeActionsConfirmation() {
        return unsafeActionsConfirmation;
    }
    public boolean showDescription() {
        return showDescription;
    }
    public void setShowDescription(boolean showDescription) {
        this.showDescription = showDescription;
    }
    public void setUnsafeActionsConfirmation(boolean unsafeActionsConfirmation) { this.unsafeActionsConfirmation = unsafeActionsConfirmation; }
    private void setApiEnabled(boolean apiEnabled) {
        this.isApiEnabled = apiEnabled;
    }
    public boolean isApiEnabled() {
        return this.isApiEnabled;
    }
    public boolean isPurgeConfirmation() {
        return purgeConfirmation;
    }
    public void setPurgeConfirmation(boolean purgeConfirmation) {
        this.purgeConfirmation = purgeConfirmation;
    }
    public boolean isApiProtectedByPassword() {
        return this.isApiProtectedByPassword;
    }
    private void setApiProtectedByPassword(boolean apiProtectedByPassword) { this.isApiProtectedByPassword = apiProtectedByPassword; }
    public boolean isBanAllowedWithoutReason() {
        return isBanAllowedWithoutReason;
    }
    public void setBanAllowedWithoutReason(boolean banAllowedWithoutReason) { isBanAllowedWithoutReason = banAllowedWithoutReason; }
    public boolean isKickAllowedWithoutReason() {
        return isKickAllowedWithoutReason;
    }
    public void setKickAllowedWithoutReason(boolean kickAllowedWithoutReason) { isKickAllowedWithoutReason = kickAllowedWithoutReason; }
    public Set<String> getPossibleGroups() {
        return timeRestrictionGroups;
    }
    public void setPossibleGroups(Set<String> timeRestrictionGroups) {
        this.timeRestrictionGroups.clear();
        this.timeRestrictionGroups = timeRestrictionGroups;
    }
    public boolean isMuteAllowedWithoutReason() {
        return isMuteAllowedWithoutReason;
    }
    public void setMuteAllowedWithoutReason(boolean muteAllowedWithoutReason) { isMuteAllowedWithoutReason = muteAllowedWithoutReason; }
    public boolean isAllowedUseRamAsContainer() {
        return this.isAllowedUseRamAsContainer;
    }
    private void setAllowedUseRamAsContainer(boolean allowedUseRamAsContainer) { this.isAllowedUseRamAsContainer = allowedUseRamAsContainer; }
    public boolean showExamples() {
        return showExamples;
    }
    public void setShowExamples(boolean showExamples) {
        this.showExamples = showExamples;
    }
    public String getStorageType() {
        return this.storageType;
    }
    private void setStorageType(String storageType) {
        this.storageType = storageType;
    }
    public String getConsoleLanguageMode() {
        return this.consoleLanguageMode;
    }
    private void setConsoleLanguageMode(String mode) {
        this.consoleLanguageMode = mode;
    }
    public boolean isAnnounceWhenLogHided() { return this.isAnnounceWhenLogHided; }
    private void setAnnounceWhenLogHided(boolean status) { this.isAnnounceWhenLogHided = status; }
    public boolean hideMainCommand() {
        return hideMainCommand;
    }
    private void setHideMainCommand(boolean hideMainCommand) {
        this.hideMainCommand = hideMainCommand;
    }
    public boolean isLessInformation() { return isLessInformation; }
    private void setLessInformation(boolean lessInformation) { isLessInformation = lessInformation; }
    public boolean isProhibitYourselfInteraction() { return isProhibitYourselfInteraction; }
    private void setProhibitYourselfInteraction(boolean prohibitYourselfInteraction) { isProhibitYourselfInteraction = prohibitYourselfInteraction; }

    public void loadConfigSettings() {

        try {
            setConsoleLanguageMode(this.fileAccessor.getGeneralConfig().getString("plugin-settings.console-language"));
            if(isLessInformation()) {
                if (this.fileAccessor.getGeneralConfig().getString("plugin-settings.console-language").equalsIgnoreCase("ru_RU")) {
                    Bukkit.getConsoleSender().sendMessage(setColors("&a[FunctionalBans] Установлен язык для консоли: ru_RU (Русский) ✔"));
                }
                if (this.fileAccessor.getGeneralConfig().getString("plugin-settings.console-language").equalsIgnoreCase("en_US")) {
                    Bukkit.getConsoleSender().sendMessage(setColors("&a[FunctionalBans] The language for the console is set: en_US (English) ✔"));
                }
            }
            if(!this.fileAccessor.getGeneralConfig().getString("plugin-settings.console-language").equalsIgnoreCase("en_US") && !this.fileAccessor.getGeneralConfig().getString("plugin-settings.console-language").equalsIgnoreCase("ru_RU")) {
                setConsoleLanguageMode("en_US");
                Bukkit.getConsoleSender().sendMessage(setColors(LangEnglish.CONFIG_UNKNOWN_LANGUAGE));
            }
        } catch (ExceptionInInitializerError ignored) {
            Bukkit.getConsoleSender().sendMessage(setColors(LangEnglish.CONFIG_LANGUAGE_ERROR));
        }

        try {
            setStorageType(this.fileAccessor.getGeneralConfig().getString("plugin-settings.storage-method"));
            if(!getStorageType().equalsIgnoreCase("sqlite") && !getStorageType().equalsIgnoreCase("h2") && !getStorageType().equalsIgnoreCase("mysql")) {
                setStorageType("sqlite");
                switch (getConsoleLanguageMode()) {
                    case "ru_RU":
                        Bukkit.getConsoleSender().sendMessage(setColors(LangRussian.CONFIG_STORAGE_METHOD_UNKNOWN.replace("%unknown_method%", this.fileAccessor.getGeneralConfig().getString("plugin-settings.storage-method"))));
                        break;
                    case "en_US":
                        Bukkit.getConsoleSender().sendMessage(setColors(LangEnglish.CONFIG_STORAGE_METHOD_UNKNOWN.replace("%unknown_method%", this.fileAccessor.getGeneralConfig().getString("plugin-settings.storage-method"))));
                        break;
                    default:
                        Bukkit.getConsoleSender().sendMessage(setColors(LangEnglish.CONFIG_STORAGE_METHOD_UNKNOWN.replace("%unknown_method%", this.fileAccessor.getGeneralConfig().getString("plugin-settings.storage-method"))));
                        break;
                }
            } else {
                if(isLessInformation()) {
                    switch (getConsoleLanguageMode()) {
                        case "ru_RU":
                            Bukkit.getConsoleSender().sendMessage(setColors(LangRussian.CONFIG_STORAGE_METHOD_LOADED.replace("%storage_method%", getStorageType())));
                            break;
                        case "en_US":
                            Bukkit.getConsoleSender().sendMessage(setColors(LangEnglish.CONFIG_STORAGE_METHOD_LOADED.replace("%storage_method%", getStorageType())));
                            break;
                        default:
                            Bukkit.getConsoleSender().sendMessage(setColors(LangEnglish.CONFIG_STORAGE_METHOD_LOADED.replace("%storage_method%", getStorageType())));
                            break;
                    }
                }
            }
        } catch (RuntimeException ignored) {
            setStorageType("sqlite");
            switch (getConsoleLanguageMode()) {
                case "ru_RU":
                    Bukkit.getConsoleSender().sendMessage(setColors(LangRussian.CONFIG_STORAGE_METHOD_LOAD_ERROR));
                    break;
                case "en_US":
                    Bukkit.getConsoleSender().sendMessage(setColors(LangEnglish.CONFIG_STORAGE_METHOD_LOAD_ERROR));
                    break;
                default:
                    Bukkit.getConsoleSender().sendMessage(setColors(LangEnglish.CONFIG_STORAGE_METHOD_LOAD_ERROR));
                    break;
            }
        }

        StaticContainers.getHidedMessagesContainer().reloadHidedMessages();
        StaticContainers.getReplacedMessagesContainer().reloadReplacedMessages();
        setLessInformation(this.fileAccessor.getGeneralConfig().getBoolean("plugin-settings.less-information"));
        setProhibitYourselfInteraction(this.fileAccessor.getGeneralConfig().getBoolean("plugin-settings.prohibit-interaction-to-yourself"));
        setBanAllowedWithoutReason(this.fileAccessor.getGeneralConfig().getBoolean("plugin-settings.reason-settings.bans-with-out-reason"));
        setKickAllowedWithoutReason(this.fileAccessor.getGeneralConfig().getBoolean("plugin-settings.reason-settings.kick-with-out-reason"));
        setMuteAllowedWithoutReason(this.fileAccessor.getGeneralConfig().getBoolean("plugin-settings.reason-settings.mute-with-out-reason"));
        setUnsafeActionsConfirmation(this.fileAccessor.getGeneralConfig().getBoolean("plugin-settings.unsafe-actions-confirmation"));
        setPurgeConfirmation(this.fileAccessor.getGeneralConfig().getBoolean("plugin-settings.purge-confirmation"));
        setShowExamples(this.fileAccessor.getGeneralConfig().getBoolean("plugin-settings.show-examples"));
        setShowDescription(this.fileAccessor.getGeneralConfig().getBoolean("plugin-settings.show-description"));
        setHideMainCommand(this.fileAccessor.getGeneralConfig().getBoolean("plugin-settings.hide-main-command"));
        this.timeRestrictionGroups.clear();
        setPossibleGroups(this.fileAccessor.getGeneralConfig().getConfigurationSection("plugin-settings.time-settings.per-groups").getKeys(false));

        setAllowedUseRamAsContainer(this.fileAccessor.getGeneralConfig().getBoolean("plugin-settings.allow-use-ram"));
        if(isAllowedUseRamAsContainer()) {
            if(isLessInformation()) {
                switch (getConsoleLanguageMode()) {
                    case "ru_RU":
                        Bukkit.getConsoleSender().sendMessage(setColors(LangRussian.CONFIG_USE_RAM_ALLOWED));
                        break;
                    case "en_US":
                        Bukkit.getConsoleSender().sendMessage(setColors(LangEnglish.CONFIG_USE_RAM_ALLOWED));
                        break;
                    default:
                        Bukkit.getConsoleSender().sendMessage(setColors(LangEnglish.CONFIG_USE_RAM_ALLOWED));
                        break;
                }
            }
        } else {
            if (isLessInformation()) {
                switch (getConsoleLanguageMode()) {
                    case "ru_RU":
                        Bukkit.getConsoleSender().sendMessage(setColors(LangRussian.CONFIG_USE_RAM_DISALLOWED));
                        break;
                    case "en_US":
                        Bukkit.getConsoleSender().sendMessage(setColors(LangEnglish.CONFIG_USE_RAM_DISALLOWED));
                        break;
                    default:
                        Bukkit.getConsoleSender().sendMessage(setColors(LangEnglish.CONFIG_USE_RAM_DISALLOWED));
                        break;
                }
            }
        }

        setAnnounceWhenLogHided(this.fileAccessor.getGeneralConfig().getBoolean("plugin-settings.console-logger.announce-console-when-message-hidden"));

        setApiEnabled(this.fileAccessor.getGeneralConfig().getBoolean("plugin-settings.api.spigot.enabled"));
        if(isLessInformation()) {
            switch (getConsoleLanguageMode()) {
                case "ru_RU": {
                    Bukkit.getConsoleSender().sendMessage(setColors(isApiEnabled() ? LangRussian.CONFIG_API_ENABLED : LangRussian.CONFIG_API_DISABLED));
                }
                case "en_US": {
                    Bukkit.getConsoleSender().sendMessage(setColors(isApiEnabled() ? LangEnglish.CONFIG_API_ENABLED : LangEnglish.CONFIG_API_DISABLED));
                }
                default: {
                    Bukkit.getConsoleSender().sendMessage(setColors(isApiEnabled() ? LangEnglish.CONFIG_API_ENABLED : LangEnglish.CONFIG_API_DISABLED));
                }
            }
        }

        setApiProtectedByPassword(this.fileAccessor.getGeneralConfig().getBoolean("plugin-settings.api.spigot.password.enabled"));
        if(isApiEnabled()) {
            if (isLessInformation()) {
                switch (getConsoleLanguageMode()) {
                    case "ru_RU": {
                        Bukkit.getConsoleSender().sendMessage(setColors(isApiProtectedByPassword() ? LangRussian.CONFIG_API_NOT_PROTECTED : LangRussian.CONFIG_API_PROTECTED));
                        break;
                    }
                    case "en_US": {
                        Bukkit.getConsoleSender().sendMessage(setColors(isApiProtectedByPassword() ? LangEnglish.CONFIG_API_NOT_PROTECTED : LangEnglish.CONFIG_API_PROTECTED));
                        break;
                    }
                    default: {
                        Bukkit.getConsoleSender().sendMessage(setColors(isApiProtectedByPassword() ? LangEnglish.CONFIG_API_NOT_PROTECTED : LangEnglish.CONFIG_API_PROTECTED));
                        break;
                    }
                }
            }
        }
    }

    public void reloadConfig() {
        try {
            setConsoleLanguageMode(this.fileAccessor.getGeneralConfig().getString("plugin-settings.console-language"));
            if(this.fileAccessor.getGeneralConfig().getString("plugin-settings.console-language").equalsIgnoreCase("ru_RU")) {
                Bukkit.getConsoleSender().sendMessage(setColors("&a[FunctionalBans] Установлен язык для консоли: ru_RU (Русский) ✔"));
            }
            if(this.fileAccessor.getGeneralConfig().getString("plugin-settings.console-language").equalsIgnoreCase("en_US")) {
                Bukkit.getConsoleSender().sendMessage(setColors("&a[FunctionalBans] The language for the console is set: en_US (English) ✔"));
            }
            if(!this.fileAccessor.getGeneralConfig().getString("plugin-settings.console-language").equalsIgnoreCase("en_US") && !this.fileAccessor.getGeneralConfig().getString("plugin-settings.console-language").equalsIgnoreCase("ru_RU")) {
                setConsoleLanguageMode("en_US");
                Bukkit.getConsoleSender().sendMessage(setColors(LangEnglish.CONFIG_UNKNOWN_LANGUAGE));
            }
        } catch (ExceptionInInitializerError ignored) {
            Bukkit.getConsoleSender().sendMessage(setColors(LangEnglish.CONFIG_LANGUAGE_ERROR));
        }
        setLessInformation(this.fileAccessor.getGeneralConfig().getBoolean("plugin-settings.less-information"));
        setProhibitYourselfInteraction(this.fileAccessor.getGeneralConfig().getBoolean("plugin-settings.prohibit-interaction-to-yourself"));
        setBanAllowedWithoutReason(this.fileAccessor.getGeneralConfig().getBoolean("plugin-settings.reason-settings.bans-with-out-reason"));
        setKickAllowedWithoutReason(this.fileAccessor.getGeneralConfig().getBoolean("plugin-settings.reason-settings.kick-with-out-reason"));
        setMuteAllowedWithoutReason(this.fileAccessor.getGeneralConfig().getBoolean("plugin-settings.reason-settings.mute-with-out-reason"));
        setUnsafeActionsConfirmation(this.fileAccessor.getGeneralConfig().getBoolean("plugin-settings.unsafe-actions-confirmation"));
        setPurgeConfirmation(this.fileAccessor.getGeneralConfig().getBoolean("plugin-settings.purge-confirmation"));
        setShowExamples(this.fileAccessor.getGeneralConfig().getBoolean("plugin-settings.show-examples"));
        setShowDescription(this.fileAccessor.getGeneralConfig().getBoolean("plugin-settings.show-description"));
        setHideMainCommand(this.fileAccessor.getGeneralConfig().getBoolean("plugin-settings.hide-main-command"));
        this.timeRestrictionGroups.clear();
        setPossibleGroups(this.fileAccessor.getGeneralConfig().getConfigurationSection("plugin-settings.time-settings.per-groups").getKeys(false));
        setAnnounceWhenLogHided(this.fileAccessor.getGeneralConfig().getBoolean("plugin-settings.console-logger.announce-console-when-message-hidden"));
    }

}
