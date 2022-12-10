package by.alis.functionalbans.API.Spigot.Events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AsyncConsoleLogOutEvent extends Event implements Cancellable {


    private boolean cancelled;
    private static final HandlerList handlerList = new HandlerList();
    private String message;
    private final boolean isApiEnabled;
    private String apiPassword;

    public AsyncConsoleLogOutEvent(String message, boolean apiEnabled) {
        super(true);
        this.message = message;
        this.isApiEnabled = apiEnabled;
    }

    /**
     * Checks if the event has been canceled
     * @return true, if event is cancelled
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Cancels this event (i.e. cancels the account ban)
     * @param cancelled true if you wish to cancel this event
     */
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * Checks whether the API is enabled in general.yml
     * @return true if API enabled
     */
    public boolean isApiEnabled() {
        return isApiEnabled;
    }

    /**
     * Used by the plugin to verify the entered password
     * @return entered password
     */
    public String getApiPassword() {
        return apiPassword;
    }

    /**
     * Used if API password protection is enabled in general.yml
     * Used to enter a password
     * @param apiPassword password to use event
     */
    public void inputApiPassword(String apiPassword) {
        this.apiPassword = apiPassword;
    }

    /**
     * Returns a message that should be sent to the console
     * @return Message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Modifies the message that is sent to the console
     * @param message New console message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    private static HandlerList getHandlerList() {
        return handlerList;
    }

    @Override
    public String getEventName() {
        return super.getEventName();
    }
}
