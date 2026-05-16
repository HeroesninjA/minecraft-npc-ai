package ro.ainpc.gui;

import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class GuiSessionManager {

    private final ConcurrentMap<UUID, GuiSession> sessionsById = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, UUID> sessionsByPlayer = new ConcurrentHashMap<>();

    public GuiSession create(Player player, GuiKey key) {
        closePlayer(player.getUniqueId());

        GuiSession session = new GuiSession(
            UUID.randomUUID(),
            player.getUniqueId(),
            key,
            System.currentTimeMillis()
        );
        sessionsById.put(session.getSessionId(), session);
        sessionsByPlayer.put(player.getUniqueId(), session.getSessionId());
        return session;
    }

    public Optional<GuiSession> find(UUID sessionId) {
        return Optional.ofNullable(sessionsById.get(sessionId));
    }

    public void close(UUID sessionId) {
        GuiSession removed = sessionsById.remove(sessionId);
        if (removed != null) {
            sessionsByPlayer.remove(removed.getPlayerId(), sessionId);
        }
    }

    public void closePlayer(UUID playerId) {
        UUID sessionId = sessionsByPlayer.remove(playerId);
        if (sessionId != null) {
            sessionsById.remove(sessionId);
        }
    }

    public void closeAll() {
        sessionsById.clear();
        sessionsByPlayer.clear();
    }
}
