package by.alis.functionalbans.spigot.Additional.Other;

import by.alis.functionalbans.spigot.FunctionalBansSpigot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static by.alis.functionalbans.spigot.Additional.GlobalSettings.StaticSettingsAccessor.getConfigSettings;


public class TextUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([a-f0-9]{6})");

    @NotNull
    public static String setColors(String inputText) {
        return translateHexColorCodes(inputText).replace("&", "§");
    }

    @NotNull
    public static String getReason(String[] arg, int num){
        StringBuilder sb = new StringBuilder();
        for(int i = num; i < arg.length; i++) {
            sb.append(arg[i]).append(" ");
        }
        return setColors(sb.toString().trim());
    }


    @Nullable
    @Deprecated
    public static String setHexColors(String inputText) {
        if (!getConfigSettings().isOldServerVersion()) {
            Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");
            Matcher matcher = pattern.matcher(inputText);
            while (matcher.find()) {
                String hexCode = inputText.substring(matcher.start(), matcher.end());
                String replaceSharp = hexCode.replace('#', 'x');
                char[] ch = replaceSharp.toCharArray();
                StringBuilder builder = new StringBuilder("");
                for (char c : ch)
                    builder.append("&").append(c);
                inputText = inputText.replace(hexCode, builder.toString());
                matcher = pattern.matcher(inputText);
            }
            return ChatColor.translateAlternateColorCodes('&', inputText);
        } else {
            return inputText;
        }
    }

    /**
     * Code by imDaniX,
     * Convert string to standardized hexadecimal format
     * @param inputText String message
     * @return Message translated to hex colors
     */
    @Nullable
    public static String translateHexColorCodes(String inputText) {
        if (!getConfigSettings().isOldServerVersion()) {
            Matcher matcher = HEX_PATTERN.matcher(inputText);
            StringBuffer buffer = new StringBuffer(inputText.length() + 4 * 8);
            while (matcher.find()) {
                String group = matcher.group(1);
                matcher.appendReplacement(buffer, ChatColor.COLOR_CHAR + "x"
                        + ChatColor.COLOR_CHAR + group.charAt(0) + ChatColor.COLOR_CHAR + group.charAt(1)
                        + ChatColor.COLOR_CHAR + group.charAt(2) + ChatColor.COLOR_CHAR + group.charAt(3)
                        + ChatColor.COLOR_CHAR + group.charAt(4) + ChatColor.COLOR_CHAR + group.charAt(5)
                );
            }
            return matcher.appendTail(buffer).toString();
        } else {
            return inputText;
        }
    }

    public static void sendSyncConsoleMessage(String message) {
        Bukkit.getScheduler().runTask(FunctionalBansSpigot.getProvidingPlugin(FunctionalBansSpigot.class), () -> {
            Bukkit.getConsoleSender().sendMessage(setColors(message));
        });
    }

}
