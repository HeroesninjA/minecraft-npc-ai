package ro.ainpc.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import ro.ainpc.AINPCPlugin;
import ro.ainpc.gui.screens.AuditGui;
import ro.ainpc.gui.screens.ConfirmActionGui;
import ro.ainpc.gui.screens.DebugGui;
import ro.ainpc.gui.screens.MainHubGui;
import ro.ainpc.gui.screens.NpcInteractionGui;
import ro.ainpc.gui.screens.NpcManagerGui;
import ro.ainpc.gui.screens.PlaceholderGui;
import ro.ainpc.gui.screens.QuestDetailGui;
import ro.ainpc.gui.screens.QuestLogGui;
import ro.ainpc.gui.screens.StatsGui;
import ro.ainpc.gui.screens.WorldHubGui;

import java.util.List;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class GuiService {

    private final AINPCPlugin plugin;
    private final GuiSessionManager sessionManager = new GuiSessionManager();
    private final Map<GuiKey, GuiScreen> screens = new EnumMap<>(GuiKey.class);
    private final ConcurrentMap<UUID, String> questDetailSelectors = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, ConfirmRequest> confirmRequests = new ConcurrentHashMap<>();

    public GuiService(AINPCPlugin plugin) {
        this.plugin = plugin;
        register(new MainHubGui());
        register(new QuestLogGui());
        register(new QuestDetailGui());
        register(new WorldHubGui());
        register(new StatsGui());
        register(new NpcInteractionGui());
        register(new NpcManagerGui());
        register(new AuditGui());
        register(new DebugGui());
        register(new ConfirmActionGui());
        register(new PlaceholderGui(GuiKey.SHOP, "Shop NPC", "Nu exista inca un serviciu shop conectat."));
    }

    public GuiSessionManager sessions() {
        return sessionManager;
    }

    public void open(Player player, GuiKey key) {
        if (player == null || key == null) {
            return;
        }
        if (!canOpen(player, key)) {
            plugin.getMessageUtils().sendMessage(player, "no_permission");
            return;
        }

        GuiScreen screen = screens.getOrDefault(key, screens.get(GuiKey.MAIN));
        GuiSession session = sessionManager.create(player, key);
        AINPCGuiHolder holder = new AINPCGuiHolder(session.getSessionId(), key);
        Inventory inventory = Bukkit.createInventory(
            holder,
            normalizeSize(screen.size(player)),
            GuiItemFactory.text(screen.title(player))
        );
        holder.attach(inventory);

        GuiRenderContext context = new GuiRenderContext(plugin, this, player, inventory);
        screen.render(context);
        session.setButtons(context.buttons());
        player.openInventory(inventory);
    }

    public boolean open(Player player, String rawKey) {
        Optional<GuiKey> key = GuiKey.fromId(rawKey);
        if (key.isEmpty()) {
            plugin.getMessageUtils().send(player, "&cGUI necunoscut: &f" + rawKey);
            return false;
        }
        open(player, key.get());
        return true;
    }

    public void openQuestDetail(Player player, String questSelector) {
        if (player == null || questSelector == null || questSelector.isBlank()) {
            plugin.getMessageUtils().sendActionBar(player, "&cQuest indisponibil.");
            return;
        }
        questDetailSelectors.put(player.getUniqueId(), questSelector);
        open(player, GuiKey.QUEST_DETAIL);
    }

    public String getQuestDetailSelector(Player player) {
        if (player == null) {
            return "";
        }
        return questDetailSelectors.getOrDefault(player.getUniqueId(), "");
    }

    public void openConfirmCommand(Player player,
                                   String title,
                                   String command,
                                   GuiKey returnKey,
                                   String returnSelector,
                                   List<String> warningLines) {
        if (player == null || command == null || command.isBlank()) {
            return;
        }

        confirmRequests.put(player.getUniqueId(), new ConfirmRequest(
            title,
            command,
            returnKey != null ? returnKey : GuiKey.MAIN,
            returnSelector,
            warningLines
        ));
        open(player, GuiKey.CONFIRM);
    }

    public Optional<ConfirmRequest> getConfirmRequest(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(confirmRequests.get(player.getUniqueId()));
    }

    public void returnFromConfirm(Player player, ConfirmRequest request) {
        if (player == null || request == null) {
            return;
        }
        confirmRequests.remove(player.getUniqueId());
        if (request.returnKey() == GuiKey.QUEST_DETAIL && !request.returnSelector().isBlank()) {
            openQuestDetail(player, request.returnSelector());
            return;
        }
        open(player, request.returnKey());
    }

    public void runConfirmedCommand(Player player, ConfirmRequest request) {
        if (player == null || request == null) {
            return;
        }
        confirmRequests.remove(player.getUniqueId());
        runCommand(player, request.command());
    }

    public void handleClick(Player player,
                            UUID sessionId,
                            int rawSlot,
                            ClickType clickType,
                            InventoryAction inventoryAction) {
        if (player == null || sessionId == null) {
            return;
        }

        Optional<GuiSession> optionalSession = sessionManager.find(sessionId);
        if (optionalSession.isEmpty()) {
            return;
        }

        GuiSession session = optionalSession.get();
        if (!session.getPlayerId().equals(player.getUniqueId())) {
            return;
        }

        GuiButton button = session.getButtons().get(rawSlot);
        if (button == null) {
            return;
        }
        if (!button.enabled() || button.action() == null) {
            plugin.getMessageUtils().sendActionBar(player, "&cButon indisponibil.");
            return;
        }

        try {
            button.action().execute(new GuiClickContext(
                plugin,
                this,
                player,
                session,
                rawSlot,
                clickType,
                inventoryAction
            ));
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Eroare la click GUI " + session.getKey().id() + ": " + exception.getMessage());
            plugin.getMessageUtils().sendActionBar(player, "&cActiunea GUI a esuat. Vezi consola.");
        }
    }

    public void runCommand(Player player, String command) {
        if (player == null || command == null || command.isBlank()) {
            return;
        }
        player.closeInventory();
        String normalized = command.startsWith("/") ? command.substring(1) : command;
        plugin.getServer().dispatchCommand(player, normalized);
    }

    public boolean canOpen(Player player, GuiKey key) {
        if (player == null || key == null) {
            return false;
        }
        return switch (key) {
            case MAIN -> hasAny(player, "ainpc.admin", "ainpc.gui");
            case QUEST, QUEST_DETAIL -> hasAny(player, "ainpc.admin", "ainpc.gui.quest", "ainpc.quest");
            case WORLD -> hasAny(player, "ainpc.admin", "ainpc.gui.world");
            case STATS -> hasAny(player, "ainpc.admin", "ainpc.gui.stats", "ainpc.info");
            case INTERACT -> hasAny(player, "ainpc.admin", "ainpc.gui.interact", "ainpc.talk");
            case SHOP -> hasAny(player, "ainpc.admin", "ainpc.gui.shop");
            case MANAGER -> hasAny(player, "ainpc.admin", "ainpc.gui.manager");
            case AUDIT -> hasAny(player, "ainpc.admin", "ainpc.gui.audit");
            case DEBUG -> hasAny(player, "ainpc.admin", "ainpc.gui.debug");
            case CONFIRM -> true;
        };
    }

    public record ConfirmRequest(
        String title,
        String command,
        GuiKey returnKey,
        String returnSelector,
        List<String> warningLines
    ) {
        public ConfirmRequest {
            title = title == null ? "Confirmare" : title;
            command = command == null ? "" : command;
            returnKey = returnKey != null ? returnKey : GuiKey.MAIN;
            returnSelector = returnSelector == null ? "" : returnSelector;
            warningLines = List.copyOf(warningLines != null ? warningLines : List.of());
        }
    }

    private void register(GuiScreen screen) {
        screens.put(screen.key(), screen);
    }

    private boolean hasAny(Player player, String... permissions) {
        for (String permission : permissions) {
            if (player.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    private int normalizeSize(int requestedSize) {
        int clamped = Math.max(9, Math.min(54, requestedSize));
        return ((clamped + 8) / 9) * 9;
    }
}
