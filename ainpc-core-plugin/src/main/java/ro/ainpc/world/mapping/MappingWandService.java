package ro.ainpc.world.mapping;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ro.ainpc.AINPCPlugin;
import ro.ainpc.gui.GuiItemFactory;
import ro.ainpc.world.WorldAdminService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MappingWandService {

    private final AINPCPlugin plugin;
    private final NamespacedKey wandKey;
    private final MappingDraftFactory draftFactory;
    private final ConcurrentMap<UUID, MappingWandSession> sessions = new ConcurrentHashMap<>();

    public MappingWandService(AINPCPlugin plugin) {
        this.plugin = plugin;
        this.wandKey = new NamespacedKey(plugin, "mapping_wand");
        this.draftFactory = new MappingDraftFactory();
    }

    public MappingWandSession start(Player player, MappingWandMode mode) {
        MappingWandMode safeMode = mode != null ? mode : MappingWandMode.PLACE;
        MappingWandSession session = sessions.compute(player.getUniqueId(), (ignored, existing) -> {
            MappingWandSelection selection = existing != null ? existing.selection() : MappingWandSelection.empty();
            MappingDraft draft = existing != null ? existing.draft() : null;
            return new MappingWandSession(safeMode, selection, draft);
        });
        giveWand(player);
        return session;
    }

    public MappingWandSession ensureSession(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(),
            ignored -> new MappingWandSession(MappingWandMode.PLACE, MappingWandSelection.empty(), null));
    }

    public Optional<MappingWandSession> session(UUID playerId) {
        return Optional.ofNullable(sessions.get(playerId));
    }

    public MappingWandMode mode(UUID playerId) {
        return session(playerId).map(MappingWandSession::mode).orElse(MappingWandMode.PLACE);
    }

    public MappingWandSession setMode(Player player, MappingWandMode mode) {
        return start(player, mode);
    }

    public MappingWandSession setPos1(Player player, MappingPoint point) {
        return updateSelection(player, selection -> selection.withPos1(point));
    }

    public MappingWandSession setPos2(Player player, MappingPoint point) {
        return updateSelection(player, selection -> selection.withPos2(point));
    }

    public MappingWandSession setPoint(Player player, MappingPoint point) {
        return updateSelection(player, selection -> selection.withPoint(point));
    }

    public MappingDraft createDraft(Player player,
                                    MappingDraftKind explicitKind,
                                    String description,
                                    WorldAdminService worldAdmin) {
        MappingWandSession session = ensureSession(player);
        MappingDraftKind kind = explicitKind != null ? explicitKind : session.mode().draftKind();
        MappingDraft draft = draftFactory.createDraft(
            player.getUniqueId(),
            kind,
            session.selection(),
            description,
            worldAdmin
        );
        sessions.put(player.getUniqueId(), new MappingWandSession(session.mode(), session.selection(), draft));
        return draft;
    }

    public MappingDraftApplyResult confirmDraft(Player player, WorldAdminService worldAdmin) {
        MappingWandSession session = ensureSession(player);
        if (session.draft() == null) {
            throw new IllegalArgumentException("Nu exista draft de confirmat. Ruleaza /ainpc map <descriere>.");
        }
        MappingDraftApplyResult result = draftFactory.apply(session.draft(), worldAdmin);
        sessions.put(player.getUniqueId(), new MappingWandSession(session.mode(), session.selection(), null));
        return result;
    }

    public void clear(UUID playerId) {
        sessions.remove(playerId);
    }

    public void cancelDraft(UUID playerId) {
        MappingWandSession session = sessions.get(playerId);
        if (session != null) {
            sessions.put(playerId, new MappingWandSession(session.mode(), session.selection(), null));
        }
    }

    public boolean isWandItem(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || !stack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        return meta != null
            && meta.getPersistentDataContainer().has(wandKey, PersistentDataType.BYTE);
    }

    public ItemStack createWandItem() {
        ItemStack stack = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(GuiItemFactory.text("&dAINPC Mapping Wand"));
            meta.lore(List.of(
                GuiItemFactory.text("&7Stanga: pos1 pentru region/place"),
                GuiItemFactory.text("&7Dreapta: pos2 pentru region/place"),
                GuiItemFactory.text("&7Node/npc_bind/quest_anchor: punct semantic"),
                GuiItemFactory.text("&8/ainpc wand mode <region|place|node|npc_bind|quest_anchor>")
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void giveWand(Player player) {
        ItemStack wand = createWandItem();
        if (player.getInventory().firstEmpty() >= 0) {
            player.getInventory().addItem(wand);
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), wand);
            plugin.getMessageUtils().send(player, "&eInventarul este plin; wand-ul a fost lasat langa tine.");
        }
    }

    private MappingWandSession updateSelection(Player player, SelectionUpdater updater) {
        return sessions.compute(player.getUniqueId(), (ignored, existing) -> {
            MappingWandSession safe = existing != null
                ? existing
                : new MappingWandSession(MappingWandMode.PLACE, MappingWandSelection.empty(), null);
            return new MappingWandSession(safe.mode(), updater.update(safe.selection()), safe.draft());
        });
    }

    @FunctionalInterface
    private interface SelectionUpdater {
        MappingWandSelection update(MappingWandSelection selection);
    }

    public record MappingWandSession(MappingWandMode mode, MappingWandSelection selection, MappingDraft draft) {
        public MappingWandSession {
            mode = mode != null ? mode : MappingWandMode.PLACE;
            selection = selection != null ? selection : MappingWandSelection.empty();
        }
    }
}
