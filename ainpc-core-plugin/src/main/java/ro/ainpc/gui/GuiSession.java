package ro.ainpc.gui;

import java.util.Map;
import java.util.UUID;

public class GuiSession {

    private final UUID sessionId;
    private final UUID playerId;
    private final GuiKey key;
    private final long openedAt;
    private Map<Integer, GuiButton> buttons = Map.of();

    public GuiSession(UUID sessionId, UUID playerId, GuiKey key, long openedAt) {
        this.sessionId = sessionId;
        this.playerId = playerId;
        this.key = key;
        this.openedAt = openedAt;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public GuiKey getKey() {
        return key;
    }

    public long getOpenedAt() {
        return openedAt;
    }

    public Map<Integer, GuiButton> getButtons() {
        return buttons;
    }

    public void setButtons(Map<Integer, GuiButton> buttons) {
        this.buttons = Map.copyOf(buttons != null ? buttons : Map.of());
    }
}
