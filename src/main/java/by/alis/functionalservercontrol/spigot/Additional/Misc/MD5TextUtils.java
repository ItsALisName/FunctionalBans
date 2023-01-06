package by.alis.functionalservercontrol.spigot.Additional.Misc;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;

import static by.alis.functionalservercontrol.spigot.Additional.Misc.TextUtils.setColors;

public class MD5TextUtils {

    /**
     * Static class
     */
    public MD5TextUtils() {}

    public static TextComponent createHoverText(String inputText, String hoverText) {
        TextComponent component = new TextComponent(setColors(inputText));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(setColors(hoverText))));
        return component;
    }

    public static TextComponent createClickableRunCommandHoverText(String inputText, String hoverText, String action) {
        TextComponent component = new TextComponent(setColors(inputText));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(setColors(hoverText))));
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, action));
        return component;
    }

    public static TextComponent createClickableRunCommandText(String inputText, String action) {
        TextComponent component = new TextComponent(setColors(inputText));
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, action));
        return component;
    }

    public static TextComponent createClickableSuggestCommandText(String inputText, String action) {
        TextComponent component = new TextComponent(setColors(inputText));
        component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, action));
        return component;
    }

    public static void sendActionBarText(Player player, String text) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(text));
    }

    public static TextComponent stringToTextComponent(String param) {
        return new TextComponent(param);
    }

    public static TextComponent appendTwo(TextComponent param1, TextComponent param2) {
        param1.addExtra(param2);
        return param1;
    }

}
